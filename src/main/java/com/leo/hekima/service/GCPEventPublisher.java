package com.leo.hekima.service;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.to.message.BaseSubsVideoMessage;
import com.leo.hekima.to.message.NoteMessageType;
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
    private final Publisher publisherNotes;
    private final Publisher publisherSubs;
    private final TaskExecutor taskExecutor;


    public GCPEventPublisher(
        @Value("${notes.publisher.credentials}") final String pubsubCredentials,
        @Value("${notes.publisher.topicid}") final String nlpsearchTopic,
        @Value("${subs.publisher.topicid}") final String subsTopic,
        TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        try {
            final var myCredentials = ServiceAccountCredentials.fromStream(
                new FileInputStream(pubsubCredentials));
                this.publisherNotes = Publisher.newBuilder(nlpsearchTopic)
                    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                    .build();
                this.publisherSubs = Publisher.newBuilder(subsTopic)
                    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                    .build();
            } catch (IOException e) {
                throw new UnrecoverableServiceException("Cannot initialize publisher", e);
            }
    }

    public void publishNoteLifeCycleMessage(final String noteUri, final NoteMessageType messageType) {
        logger.debug("Publishing message for note {}", noteUri);
        try {
            final PubSubMessage payload = new PubSubMessage(noteUri, messageType.name());
            final PubsubMessage message = PubsubMessage.newBuilder().setData(
                ByteString.copyFromUtf8(JsonUtils.serializeSilentFail(payload))
            ).build();
            final ApiFuture<String> futureMessage = publisherNotes.publish(message);
            futureMessage.addListener(() -> logger.debug("Message was pushed on GCP Pub/Sub : {}", futureMessage.isDone()), taskExecutor);
        } catch (Exception e) {
            logger.error("Error while publishing message about note {} of type {}", noteUri, messageType, e);
        }
    }

    @Override
    public void publishSubMessage(BaseSubsVideoMessage payload) {
        logger.debug("Publishing message of type {} for subs {}", payload.getType(), payload.getVideoName());
        try {
            final PubsubMessage message = PubsubMessage.newBuilder().setData(
                ByteString.copyFromUtf8(JsonUtils.serializeSilentFail(payload))
            )
            .putAttributes("type", payload.getType().name())
            .build();
            final ApiFuture<String> futureMessage = publisherSubs.publish(message);
            futureMessage.addListener(() -> logger.debug("Message was pushed on GCP Pub/Sub : {}", futureMessage.isDone()), taskExecutor);
        } catch (Exception e) {
            logger.error("Error while publishing message about {}", payload.getVideoName(), e);
        }
    }
}
