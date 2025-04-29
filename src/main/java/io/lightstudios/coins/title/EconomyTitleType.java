package io.lightstudios.coins.title;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public enum EconomyTitleType {

    DEPOSIT_COINS("coins.onDeposit"),
    WITHDRAW_COINS("coins.onWithdraw"),
    PAY_SENDER_COINS("coins.onPay.sender"),
    PAY_TARGET_COINS("coins.onPay.target"),

    DEPOSIT_VIRTUAL("virtual.onDeposit"),
    WITHDRAW_VIRTUAL("virtual.onWithdraw"),

    ;

    private final String type;

    EconomyTitleType(String type) {
        this.type = type;
    }

    @Nullable
    public String getPath() {
        String[] path = this.type.split("\\.");

        if(path.length == 2) {
            return path[1];
        }

        if(path.length == 3) {
            return path[1] + "." + path[2];
        }
        return null;
    }

}
