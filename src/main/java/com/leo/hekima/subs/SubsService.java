package com.leo.hekima.subs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.utils.RequestUtils;
import com.leo.hekima.utils.StringUtils;
import com.leo.hekima.utils.WebUtils;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.leo.hekima.subs.SearchPattern.*;

@Component
public class SubsService {
    private static final Logger logger = LoggerFactory.getLogger(SubsService.class);
    private final Komoran komoran;
    private List<SubsDbEntry> corpus;
    private Multimap<PosTag, IndexEntry> db = HashMultimap.create(1, 1);
    private final String subsStorePath;

    public SubsService(@Value("${subs.store.path}") final String subsStorePath) {
        logger.info("Loading Komoran...");
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);
        logger.info("Komoran loaded");
        this.corpus = Collections.emptyList();
        this.subsStorePath = subsStorePath;
        reloadDb();
    }

    public void reloadDb() {
        final File subsStore = new File(subsStorePath);
        ensureExistsAndReadable(subsStore);
        final File[] directories = subsStore.listFiles(File::isDirectory);
        final var corpus = new ArrayList<SubsDbEntry>();
        Multimap<PosTag, IndexEntry> db = HashMultimap.create();
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

    public Mono<ServerResponse> search(final ServerRequest serverRequest) {
        final String query = serverRequest.queryParam("q").orElse("");
        final float minSimilarity = Float.parseFloat(serverRequest.queryParam("minSim").orElse("0.75"));
        final float maxSimilarity = Float.parseFloat(serverRequest.queryParam("maxSim").orElse("1"));
        final boolean excludeMax = Boolean.parseBoolean(serverRequest.queryParam("exclMax").orElse("false"));
        logger.info("Looking for {}", query);
        final List<PosTag> analyzedQuery = toSentence(query, komoran);
        final float minScore = getMaxScore(analyzedQuery) * minSimilarity;
        final float maxScore = getMaxScore(analyzedQuery) * maxSimilarity;
        final List<Integer> firstCandidates = findFixMatches(analyzedQuery, this.db, minSimilarity);
        final List<IndexWithScoreAndZone> matches = scoreSentencesAgainstQuery(analyzedQuery, firstCandidates,
                this.corpus.stream().map(SubsDbEntry::tags).collect(Collectors.toList()));
        final List<SubsEntryView> results = matches.stream()
        .filter(m -> m.score() >= minScore && (excludeMax ? m.score() < maxScore : m.score() <= maxScore))
        .sorted((m1, m2) -> {
            float delta = m2.score() - m1.score();
            if(delta < 0) {
                return -1;
            } if(delta > 0) {
                return 1;
            }
            return 0;
        })
        .map(m -> {
            final SubsDbEntry sub = this.corpus.get(m.sentenceIndex());
            return new SubsEntryView(sub.videoName(), sub.subs(), sub.fromTs(), sub.toTs(), m.from(), m.to());
        })
        .collect(Collectors.toList());
        return WebUtils.ok().bodyValue(results);
    }

    private List<SubsDbEntry> loadSubsFromFile(final File csvFile) {
        final String prefix = csvFile.getName().replace(".csv", "");
        try (CSVReader reader = new CSVReader(new FileReader(csvFile, StandardCharsets.UTF_8))) {
            List<String[]> lines = reader.readAll();
            return lines.stream().skip(1)
                .map(line -> {
                    List<PosTag> tags = new ArrayList<>();
                    for (int i = 3; i < line.length - 1; i+=2) {
                        final String content = line[i];
                        if(StringUtils.isNotEmpty(content)) {
                            tags.add(new PosTag(content, line[i + 1]));
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
        return WebUtils.ok().bodyValue("done");
    }
    public static Multimap<PosTag, IndexEntry> index(final List<SubsDbEntry> corpus) {
        final Multimap<PosTag, IndexEntry> db = HashMultimap.create();
        for (int i = 0; i < corpus.size(); i++) {
            final List<PosTag> sentence = corpus.get(i).tags();
            for (int indexTag = 0; indexTag < sentence.size(); indexTag++) {
                final PosTag posTag = sentence.get(indexTag);
                db.put(posTag, new IndexEntry(i, indexTag));
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
        return WebUtils.ok().bodyValue(hints.distinct().skip(offset).limit(count).map(SubsDbEntry::subs).collect(Collectors.toList()));
    }
}
