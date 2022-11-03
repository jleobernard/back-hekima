package com.leo.hekima.service;

import com.leo.hekima.to.message.BaseSubsVideoMessage;
import com.leo.hekima.to.message.NoteMessageType;

public interface EventPublisher {
    void publishNoteLifeCycleMessage(final String noteUri, final NoteMessageType messageType);
    void publishSubMessage(final BaseSubsVideoMessage payload);
}
