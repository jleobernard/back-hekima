package com.leo.hekima.service;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.to.NoteMessageType;
import com.leo.hekima.to.PubSubMessage;
import com.leo.hekima.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;

@Service
@ConditionalOnProperty(name = "notes.publisher.strategy", havingValue = "gcp")
public class GCPEventPublisher implements EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(GCPEventPublisher.class);
    private final Publisher publisher;
    private final TaskExecutor taskExecutor;


    public GCPEventPublisher(
        @Value("${notes.publisher.credentials}") final String pubsubCredentials,
        @Value("${notes.publisher.topicid}") final String nlpsearchTopic,
        TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        try {
            final var myCredentials = ServiceAccountCredentials.fromStream(
                new FileInputStream(pubsubCredentials));
                this.publisher = Publisher.newBuilder(nlpsearchTopic)
                    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                    .build();
            } catch (IOException e) {
                throw new UnrecoverableServiceException("Cannot initialize publisher", e);
            }
    }

    public void publishMessage(final String noteUri, final NoteMessageType messageType) {
        logger.debug("Publishing message for note {}", noteUri);
        try {
            final PubSubMessage payload = new PubSubMessage(noteUri, messageType.name());
            final PubsubMessage message = PubsubMessage.newBuilder().setData(
                ByteString.copyFromUtf8(JsonUtils.serializeSilentFail(payload))
            ).build();
            final ApiFuture<String> futureMessage = publisher.publish(message);
            futureMessage.addListener(() -> logger.debug("Message was pushed on GCP Pub/Sub : {}", futureMessage.isDone()), taskExecutor);
        } catch (Exception e) {
            logger.error("Error while publishing message about note {} of type {}", noteUri, messageType, e);
        }
    }
}
