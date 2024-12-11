package io.lightstudios.coins.permissions;

import lombok.Getter;

@Getter
public enum LightPermissions {



    /*
        Admin Command Perissions
     */

    COINS_COMMAND("lightcoins.command.player.coins"),


    /*
        Player Command Permissions
     */

    COINS_ADD_COMMAND("lightcoins.command.admin.coins.add"),
    COINS_REMOVE_COMMAND("lightcoins.command.admin.coins.remove"),

            ;

    private final String perm;
    LightPermissions(String perm) { this.perm = perm; }
}
