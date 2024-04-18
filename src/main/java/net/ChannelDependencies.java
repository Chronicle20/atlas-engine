package net;

import java.util.Objects;

import client.processor.npc.FredrickProcessor;

public record ChannelDependencies(FredrickProcessor fredrickProcessor) {

    public ChannelDependencies {
        Objects.requireNonNull(fredrickProcessor);
    }
}
