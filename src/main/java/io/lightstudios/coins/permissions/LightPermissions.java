package io.lightstudios.coins.permissions;

import lombok.Getter;

@Getter
public enum LightPermissions {



    /*
        Admin Command Perissions
     */

    COINS_ADD_COMMAND("lightcoins.command.admin.coins.add"),
    COINS_REMOVE_COMMAND("lightcoins.command.admin.coins.remove"),
    RELOAD_COMMAND("lightcoins.command.admin.reload"),
    DELETE_ACCOUNT_COMMAND("lightcoins.command.admin.delete"),


    /*
        Player Command Permissions
     */

    COINS_COMMAND("lightcoins.command.player.coins"),
    PAY_COMMAND("lightcoins.command.player.coins.remove"),
    BALTOP_COMMAND("lightcoins.command.player.baltop"),

            ;

    private final String perm;
    LightPermissions(String perm) { this.perm = perm; }
}
