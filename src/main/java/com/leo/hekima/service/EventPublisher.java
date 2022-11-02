package com.leo.hekima.service;

import com.leo.hekima.to.NoteMessageType;

public interface EventPublisher {
    void publishMessage(final String noteUri, final NoteMessageType messageType);
}
