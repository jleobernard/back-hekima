package com.leo.hekima.to.message;

public class BaseSubsVideoMessage {
    private final SubsMessageType type;
    private final String videoName;

    public BaseSubsVideoMessage(final String videoName, final SubsMessageType type) {
        this.type = type;
        this.videoName = videoName;
    }

    public SubsMessageType getType() {
        return type;
    }

    public String getVideoName() {
        return videoName;
    }
}
