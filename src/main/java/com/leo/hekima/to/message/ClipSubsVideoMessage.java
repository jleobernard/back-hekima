package com.leo.hekima.to.message;

public class ClipSubsVideoMessage extends BaseSubsVideoMessage {
    private final int from;
    private final int to;
    protected ClipSubsVideoMessage(final String videoName,
                                   final int from,
                                   final int to) {
        super(videoName, SubsMessageType.CLIP);
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }
}
