package io.lightstudios.coins.permissions;

import lombok.Getter;

@Getter
public enum LightPermissions {



    /*
        Admin Command Perissions
     */

    COINS_ADD_COMMAND("lightcoins.command.admin.coins.add"),
    COINS_SET_COMMAND("lightcoins.command.admin.coins.set"),
    COINS_REMOVE_COMMAND("lightcoins.command.admin.coins.remove"),
    COINS_SHOW_COMMAND("lightcoins.command.admin.coins.show"),
    COINS_HELP_ADMIN_COMMAND("lightcoins.command.admin.coins.help"),
    RELOAD_COMMAND("lightcoins.command.admin.reload"),
    DEFAULT_HELP_COMMAND("lightcoins.command.admin.help"),
    DELETE_ACCOUNT_COMMAND("lightcoins.command.admin.delete"),
    ADD_ALL_COMMAND("lightcoins.command.admin.coins.addall"),
    VIRTUAL_ADD_COMMAND("lightcoins.command.admin.virtual.add"),
    VIRTUAL_SET_COMMAND("lightcoins.command.admin.virtual.set"),
    VIRTUAL_REMOVE_COMMAND("lightcoins.command.admin.virtual.remove"),
    VIRTUAL_HELP_COMMAND("lightcoins.command.admin.virtual.help"),


    /*
        Player Command Permissions
     */

    PAY_COMMAND("lightcoins.command.player.coins.pay"),
    BALTOP_COMMAND("lightcoins.command.player.coins.baltop"),
    VIRTUAL_SHOW_COMMAND("lightcoins.command.player.virtual.show"),
            ;

    private final String perm;
    LightPermissions(String perm) { this.perm = perm; }
}
