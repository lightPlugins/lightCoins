package io.lightstudios.coins.api.types;

import lombok.Getter;

@Getter
public enum VirtualReason {

    DEFAULT("Unknown"),
    CORE("Core"),
    NEW_ACCOUNT("Server"),
    ADD_COMMAND("Command"),
    REMOVE_COMMAND("Command"),
    SET_COMMAND("Command"),

    ;

    private final String name;

    VirtualReason(String name) {
        this.name = name;
    }
}
