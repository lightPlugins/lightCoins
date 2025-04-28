package io.lightstudios.coins.api.types;

import lombok.Getter;

@Getter
public enum EconomyReason {

    DEFAULT("Unknown"),
    CORE("Core"),
    NEW_ACCOUNT("Server"),
    ADD_COMMAND("Command"),
    REMOVE_COMMAND("Command"),
    SET_COMMAND("Command"),
    ADD_ALL_COMMAND("Command"),
    PAY_COMMAND("Command"),
    ON_DEATH("Death"),

    ADD_COMMAND_CONSOLE("Console"),
    REMOVE_COMMAND_CONSOLE("Console"),
    SET_COMMAND_CONSOLE("Console"),

    IMPLEMENTER_DEPOSIT("External"),
    IMPLEMENTER_WITHDRAW("External"),

    ;

    private final String name;

    EconomyReason(String name) {
        this.name = name;
    }
}
