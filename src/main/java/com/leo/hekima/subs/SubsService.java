package com.leo.hekima.subs;

import com.leo.hekima.exception.UnrecoverableServiceException;
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

@Component
public class SubsService {
    private static final Logger logger = LoggerFactory.getLogger(SubsService.class);
    private final Komoran komoran;
    private List<SubsDbEntry> db;
    private final String subsStorePath;

    public SubsService(@Value("${subs.store.path}") final String subsStorePath) {
        logger.info("Loading Komoran...");
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);
        logger.info("Komoran loaded");
        this.db = new ArrayList<>();
        this.subsStorePath = subsStorePath;
        reloadDb();
    }

    public void reloadDb() {
        final File subsStore = new File(subsStorePath);
        ensureExistsAndReadable(subsStore);
        final File[] directories = subsStore.listFiles(File::isDirectory);
        final var db = new ArrayList<SubsDbEntry>();
        for (File directory : directories) {
            final String prefix = directory.getName();
            final File csvFile = new File(directory, prefix + ".csv");
            if(csvFile.exists()) {
                final List<SubsDbEntry> entries = loadSubsFromFile(csvFile);
                if(entries.isEmpty()) {
                    logger.info("No entries in " + csvFile.getAbsolutePath());
                } else {
                    db.addAll(entries);
                    logger.info("{} entries loadded from " + csvFile.getAbsolutePath(), entries.size());
                }
            } else {
                logger.info(directory.getAbsolutePath() + " does not exist");
            }
        }
        logger.info("{} subs loaded", db.size());
        this.db = db;
    }

    public Mono<ServerResponse> search(final ServerRequest serverRequest) {
        final String query = serverRequest.queryParam("q").orElse("");
        logger.info("Looking for {}", query);
        final var searchPattern = new SearchPattern(query, this.komoran);
        final var fixWords = searchPattern.getFixWords();
        final var candidates =
            this.db.stream().filter(entry -> entry.hasEveryWord(entry, fixWords))
            .collect(Collectors.toList());
        final List<SubsEntryView> results = candidates.stream().filter(candidate -> {
            /*
            # La boucle suivante peut-être optimisée pour savoir à quel index on
            # pourrait reprendre après avoir arrêté à un certain état de la machine
            # à état mais 1/ c'est long à faire 2/ pas sûr qu'on ait de meilleurs
            # résultats comme les phrases et les requêtes sont relativement petites
             */
            return searchPattern.matches(candidate.tags());
        })
        .map(m -> new SubsEntryView(m.videoName(), m.subs(), m.fromTs(), m.toTs()))
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
}
