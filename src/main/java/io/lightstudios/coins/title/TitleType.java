package io.lightstudios.coins.title;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public enum TitleType {

    DEPOSIT_COINS("coins.onDeposit"),
    WITHDRAW_COINS("coins.onWithdraw"),

    DEPOSIT_VIRTUAL("virtual.onDeposit"),
    WITHDRAW_VIRTUAL("virtual.onWithdraw"),

    ;

    private final String type;

    TitleType(String type) {
        this.type = type;
    }

    @Nullable
    public String getPath() {
        String[] path = TitleType.class.getName().split("\\.");

        if(path.length > 1) {
            return path[1];
        }
        return null;
    }

}
