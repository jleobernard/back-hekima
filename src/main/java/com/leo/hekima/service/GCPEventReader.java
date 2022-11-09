package com.leo.hekima.service;

import com.google.cloud.pubsub.v1.Subscriber;
import com.leo.hekima.subs.SubsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "subs.reader.strategy", havingValue = "gcp")
public class GCPEventReader {
    private static final Logger logger = LoggerFactory.getLogger(GCPEventReader.class);


    public GCPEventReader(@Value("${subs.reader.subscription}") final String subscription, SubsService subsService) {
        Subscriber.newBuilder(subscription, (message, consumer) -> {
            consumer.ack();
            logger.info("Launching reload of subs db");
            subsService.reloadDb();
            logger.info("Subs db reloaded");
        })
        .build()
        .startAsync();
        logger.info("Listening for events at {}", subscription);
    }
}
