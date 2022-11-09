package com.leo.hekima.subs;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.service.EventPublisher;
import com.leo.hekima.to.AckResponse;
import com.leo.hekima.to.SubsSearchPatternElement;
import com.leo.hekima.to.SubsSearchRequest;
import com.leo.hekima.to.message.BaseSubsVideoMessage;
import com.leo.hekima.to.message.SubsMessageType;
import com.leo.hekima.utils.RequestUtils;
import com.leo.hekima.utils.StringUtils;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.leo.hekima.subs.SearchPattern.*;
import static com.leo.hekima.utils.WebUtils.ok;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@Component
public class SubsService {
    private static final Logger logger = LoggerFactory.getLogger(SubsService.class);
    public static final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    private final Storage storage;
    private final String bucketName;
    private List<SubsDbEntry> corpus;
    private Multimap<SentenceElement, IndexEntry> db = HashMultimap.create(1, 1);
    private final String subsStorePath;

    private final EventPublisher eventPublisher;

    public SubsService(@Value("${subs.store.path}") final String subsStorePath,
                       @Value("${subs.cloudstorage.projectId}") final String projectId,
                       @Value("${subs.cloudstorage.bucketId}") final String bucketId,
                       final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.corpus = Collections.emptyList();
        this.subsStorePath = subsStorePath;
        if(subsStorePath != null) {
            reloadDb();
        }
        if(projectId == null) {
            this.storage = null;
            this.bucketName = null;
        } else {
            this.storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build().getService();
            this.bucketName = bucketId;
        }

    }

    public static SubsService fromMemory(final String... haystack) {
        final SubsService ss = new SubsService(null, null, null, null);
        final List<Pair<String, List<SentenceElement>>> entries = Arrays.stream(haystack)
            .map(line ->
                Pair.of(line, komoran.analyze(line).getList().stream()
                    .map(elt -> new SentenceElement(Optional.of(elt.getFirst()), Optional.of(elt.getSecond())))
                    .toList()
                )
            ).toList();
        ss.corpus = entries.stream().map(_entries ->
            new SubsDbEntry("in-mem", _entries.getFirst(), -1, -1, _entries.getSecond())
        ).toList();
        ss.db = index(ss.corpus);
        return ss;
    }

    public void reloadDb() {
        final File subsStore = new File(subsStorePath);
        ensureExistsAndReadable(subsStore);
        final File[] directories = subsStore.listFiles(File::isDirectory);
        final var corpus = new ArrayList<SubsDbEntry>();
        Multimap<SentenceElement, IndexEntry> db = HashMultimap.create();
        if(directories != null) {
            for (File directory : directories) {
                final String prefix = directory.getName();
                final File csvFile = new File(directory, prefix + ".csv");
                if (csvFile.exists()) {
                    final List<SubsDbEntry> entries = loadSubsFromFile(csvFile);
                    if (entries.isEmpty()) {
                        logger.info("No entries in " + csvFile.getAbsolutePath());
                    } else {
                        corpus.addAll(entries);
                        logger.info("{} entries loadded from " + csvFile.getAbsolutePath(), entries.size());
                    }
                } else {
                    logger.info(directory.getAbsolutePath() + " does not exist");
                }
            }
            db = index(corpus);
        }
        logger.info("{} subs loaded", corpus.size());
        this.corpus = corpus;
        this.db = db;
    }

    public Mono<ServerResponse> explain(final ServerRequest serverRequest) {
        final String query = serverRequest.queryParam("q").orElse("");
        //final List<PosTag> analyzedQuery = toSentence(query, komoran);
        //return WebUtils.ok().bodyValue(analyzedQuery);
        return null;
    }

    public Mono<ServerResponse> search(final ServerRequest serverRequest) {
        final String query = serverRequest.queryParam("q").orElse("");
        final boolean exactQuery = serverRequest.queryParam("exact").map(Boolean::parseBoolean).orElse(false);
        if(exactQuery) {
            final var hints = corpus.stream().filter(c -> c.subs().contains(query));
            return ok().bodyValue(hints.distinct()
                .map(sub -> {
                    final var subValue = sub.subs();
                    final int indexMatch = subValue.indexOf(query);
                    return new SubsEntryView(sub.videoName(), subValue, sub.fromTs(), sub.toTs(), indexMatch,
                        indexMatch + query.length());
                })
                .collect(Collectors.toList()));
        } else {
            final SubsSearchRequest subsSearchRequest = parseQuery(serverRequest);
            logger.debug("Looking for {}", query);
            final List<Sentence> analyzedQueries = toSentences(subsSearchRequest, komoran);
            final List<IndexWithScoreAndZone> results = new ArrayList<>();
            for (Sentence analyzedQuery : analyzedQueries) {
                final SubsSearchProblem problem = new SubsSearchProblem(subsSearchRequest, analyzedQuery,
                    getMaxScore(analyzedQuery) * subsSearchRequest.minSimilarity(),
                    getMaxScore(analyzedQuery) * subsSearchRequest.maxSimilarity()
                );
                final List<Integer> firstCandidates = findFixMatches(problem, this.db);
                final List<IndexWithScoreAndZone> matches = scoreSentencesAgainstQuery(analyzedQuery, firstCandidates,
                    this.corpus.stream().map(SubsDbEntry::tags).collect(Collectors.toList()));
                results.addAll(matches.stream()
                    .filter(m -> m.score() >= problem.minScore() && (problem.request().excludeMax()) ?
                        m.score() < problem.maxScore() : m.score() <= problem.maxScore()).toList());
            }
            return ok().bodyValue(results.stream().sorted((m1, m2) -> {
                    float delta = m2.score() - m1.score();
                    if (delta < 0) {
                        return -1;
                    }
                    if (delta > 0) {
                        return 1;
                    }
                    return 0;
                })
                .map(m -> {
                    final SubsDbEntry sub = this.corpus.get(m.sentenceIndex());
                    return new SubsEntryView(sub.videoName(), sub.subs(), sub.fromTs(), sub.toTs(), m.from(), m.to());
                }));
        }
    }

    public static SubsSearchRequest parseQuery(ServerRequest serverRequest) {
        final String q = serverRequest.queryParam("q").orElse("");
        final float minSimilarity = Float.parseFloat(serverRequest.queryParam("minSim").orElse("0.75"));
        final float maxSimilarity = Float.parseFloat(serverRequest.queryParam("maxSim").orElse("1"));
        final boolean excludeMax = Boolean.parseBoolean(serverRequest.queryParam("exclMax").orElse("false"));
        return new SubsSearchRequest(q, serverRequest.queryParam("exact").map(Boolean::parseBoolean).orElse(false),
                parseQueryElements(q), minSimilarity, maxSimilarity, excludeMax);
    }

    public static SubsSearchPatternElement[] parseQueryElements(final String q) {
        if(org.apache.commons.lang3.StringUtils.isBlank(q)) {
            return new SubsSearchPatternElement[0];
        }
        final String[] splitted = q.trim().split("\\+");
        SubsSearchPatternElement[] parsed = new SubsSearchPatternElement[splitted.length];
        for (int i = 0; i < splitted.length; i++) {
            final String[] alternativesAndTag = splitted[i].split(":", 2);
            final String[] alternatives;
            final String posTag;
            switch (alternativesAndTag.length) {
                case 0 -> throw new IllegalArgumentException("query.element.empty." + i);
                case 1 -> {
                    alternatives = alternativesAndTag[0].split("/");
                    posTag = "";
                }
                case 2 -> {
                    alternatives = alternativesAndTag[0].split("/");
                    posTag = alternativesAndTag[1].trim().toUpperCase();
                }
                default -> throw new IllegalArgumentException("should.never.happen.because.of.split.limit");
            }
            parsed[i] =
                new SubsSearchPatternElement((alternatives == null || alternatives.length == 0 ||
                    (alternatives.length == 1 && org.apache.commons.lang3.StringUtils.isEmpty(alternatives[0]))) ?
                    Optional.empty() : Optional.of(alternatives),
                SearchableType.fromTag(posTag));
        }
        return parsed;
    }

    private List<SubsDbEntry> loadSubsFromFile(final File csvFile) {
        final String prefix = csvFile.getName().replace(".csv", "");
        try (CSVReader reader = new CSVReader(new FileReader(csvFile, StandardCharsets.UTF_8))) {
            List<String[]> lines = reader.readAll();
            return lines.stream().skip(1)
                .map(line -> {
                    List<SentenceElement> tags = new ArrayList<>();
                    for (int i = 3; i < line.length - 1; i+=2) {
                        final String content = line[i];
                        if(StringUtils.isNotEmpty(content)) {
                            String type = line[i + 1];
                            tags.add(new SentenceElement(content, isEmpty(type) ? "???" : type.trim()));
                        } else {
                            break;
                        }
                    }
                    return new SubsDbEntry(prefix, line[0],
                            Float.parseFloat(line[1]),
                            Float.parseFloat(line[2]),
                            tags);
                }).collect(Collectors.toList());
        } catch (IOException | CsvException e) {
            logger.error("Error while analyzing file {}", csvFile.getAbsoluteFile());
        }
        return Collections.emptyList();
    }

    private void ensureExistsAndReadable(File subsStore) {
        if(!subsStore.exists()) {
            throw new UnrecoverableServiceException(subsStore.getAbsolutePath() + " should exists");
        }
        if(!subsStore.canRead()) {
            throw new UnrecoverableServiceException(subsStore.getAbsolutePath() + " not readable");
        }
    }

    public Mono<ServerResponse> askReloadDb(ServerRequest request) {
        reloadDb();
        return ok().bodyValue("done");
    }
    public static Multimap<SentenceElement, IndexEntry> index(final List<SubsDbEntry> corpus) {
        final Multimap<SentenceElement, IndexEntry> db = HashMultimap.create();
        for (int i = 0; i < corpus.size(); i++) {
            final List<SentenceElement> sentence = corpus.get(i).tags();
            for (int indexTag = 0; indexTag < sentence.size(); indexTag++) {
                final SentenceElement sentenceElement = sentence.get(indexTag);
                if(sentenceElement.type().isEmpty()) {
                    logger.info("{} has no type", sentenceElement);
                } else {
                    db.put(sentenceElement, new IndexEntry(i, indexTag));
                }
            }
        }
        return db;
    }

    public Mono<ServerResponse> autocomplete(ServerRequest serverRequest) {
        final String query = serverRequest.queryParam("q").orElse("");
        final int count = RequestUtils.getCount(serverRequest);
        final int offset = RequestUtils.getOffset(serverRequest);
        final Stream<SubsDbEntry> hints;
        if(query.equals("")) {
            hints = corpus.stream();
        } else {
            hints = corpus.stream().filter(c -> c.subs().contains(query));
        }
        return ok().bodyValue(hints.distinct().skip(offset).limit(count).map(SubsDbEntry::subs).collect(Collectors.toList()));
    }

    public Mono<ServerResponse> text(ServerRequest serverRequest) {
        final String videoName = serverRequest.pathVariable("videoName");
        final float from = Float.parseFloat(serverRequest.queryParam("from").orElse(""));
        final float to = Float.parseFloat(serverRequest.queryParam("to").orElse(""));
        final List<SubsTextView> entries = corpus.stream().filter(entry -> videoName.equals(entry.videoName()))
            .filter(entry -> {
                var minBound = Math.max(from, entry.fromTs());
                var maxBound = Math.min(to, entry.toTs());
                return minBound <= maxBound;
            })
            .sorted((e1, e2) -> Float.compare(e1.fromTs(), e2.fromTs()))
            .map(this::toText)
            .toList();
        return ok().bodyValue(entries);
    }

    private SubsTextView toText(final SubsDbEntry subsDbEntry) {
        return new SubsTextView(subsDbEntry.subs(), subsDbEntry.fromTs(), subsDbEntry.toTs());
    }

    public static Multimap<SentenceElement, IndexEntry> createDbFromSentences(final String... corpus) {
        final List<List<SentenceElement>> entries = Arrays.stream(corpus)
            .map(line ->
                komoran.analyze(line).getList().stream()
                    .map(elt -> new SentenceElement(Optional.of(elt.getFirst()), Optional.of(elt.getSecond())))
                    .toList()
            ).toList();
        return SearchPattern.index(entries);
    }

    public Mono<ServerResponse> upload(final ServerRequest serverRequest) {
        logger.info("Upload new video file with subs...");
        return serverRequest.multipartData().doOnNext(multipartData -> {
            final List<Part> parts = multipartData.get("file");
            final Part file = parts.get(0);
            final FilePart filePart = ((FilePart) file);
            final String fileName = filePart.filename();
            logger.info("File name is {}", fileName);
            final File destination = Path.of("/tmp", fileName).toFile();
            filePart.transferTo(destination).then(Mono.defer(() -> {
                final BlobId blobId = BlobId.of(bucketName, getBlobPathFromName(fileName));
                final BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                try {
                    logger.info("Sending content bytes ...");
                    try (WriteChannel writer = storage.writer(blobInfo)) {
                        byte[] buffer = new byte[10_240];
                        try (InputStream input = Files.newInputStream(destination.toPath())) {
                            int limit;
                            while ((limit = input.read(buffer)) >= 0) {
                                writer.write(ByteBuffer.wrap(buffer, 0, limit));
                            }
                        }

                    }
                    logger.info("... upload done");
                    logger.info("Sending message to notify of new video {}", fileName);
                    eventPublisher.publishSubMessage(new BaseSubsVideoMessage(fileName, SubsMessageType.NEW));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return Mono.empty();
            }))
            .doOnTerminate(() -> {
                if(destination.exists()) {
                    logger.debug("Deleting temp file " + destination.getAbsolutePath());
                    if(destination.delete()) {
                        logger.info("Temp file " + destination.getAbsolutePath() + " deleted");
                    } else {
                        logger.warn("Error while deleting temp file " + destination.getAbsolutePath());
                    }
                }
            })
            .subscribe();
        }).flatMap(e -> ok().bodyValue(AckResponse.OK));
    }

    public static String getBlobPathFromName(final String fileName) {
        final String baseName = FilenameUtils.getBaseName(fileName);
        return "videos/" + baseName + "/" + fileName;
    }
}
