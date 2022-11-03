package com.leo.hekima.service;

import com.leo.hekima.to.message.BaseSubsVideoMessage;
import com.leo.hekima.to.message.NoteMessageType;
import com.leo.hekima.to.PubSubMessage;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.leo.hekima.utils.JsonUtils.serializeSilentFail;
import static com.leo.hekima.utils.StringUtils.base64EncodeJson;

@Service
@ConditionalOnProperty(name = "notes.publisher.strategy", havingValue = "rest")
public class RestEventPublisher implements EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(RestEventPublisher.class);
    private final WebClient nlpsearchWebClient;

    public RestEventPublisher(@Value("${nlpsearch.url}") final String nlpsearchUrl) {
        this.nlpsearchWebClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .baseUrl(nlpsearchUrl)
            .build();
    }

    public void publishNoteLifeCycleMessage(final String noteUri, final NoteMessageType messageType) {
        logger.debug("Publishing message for note {}", noteUri);
        try {
            final PubSubMessage payload = new PubSubMessage(noteUri, messageType.name());
            nlpsearchWebClient.post()
                .uri("/api/notes/notifications")
                .bodyValue(
                    serializeSilentFail(
                        Map.of("message", Map.of("data", base64EncodeJson(payload)))
                    )
                ).exchangeToMono(clientResponse -> {
                    System.out.println(clientResponse);
                    return Mono.empty();
                }).subscribe();
        } catch (Exception e) {
            logger.error("Error while publishing message about note {} of type {}", noteUri, messageType, e);
        }
    }

    @Override
    public void publishSubMessage(BaseSubsVideoMessage payload) {
        throw new NotImplementedException();
    }
}
