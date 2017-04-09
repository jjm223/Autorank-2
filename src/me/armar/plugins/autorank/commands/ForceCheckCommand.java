package me.armar.plugins.autorank.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.commands.manager.AutorankCommand;
import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.permissions.AutorankPermission;
import me.armar.plugins.autorank.util.AutorankTools;

/**
 * The command delegator for the '/ar fcheck' command.
 */
public class ForceCheckCommand extends AutorankCommand {

    private final Autorank plugin;

    public ForceCheckCommand(final Autorank instance) {
         plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {

        if (!plugin.getCommandsManager().hasPermission( AutorankPermission.FORCE_CHECK, sender))
            return true;

        if (args.length != 2) {
            sender.sendMessage(Lang.INVALID_FORMAT.getConfigValue("/ar forcecheck <player>"));
            return true;
        }

        final String target = args[1];
        final Player targetPlayer = plugin.getServer().getPlayer(target);

        if (targetPlayer == null) {
            sender.sendMessage(Lang.PLAYER_NOT_ONLINE.getConfigValue(target));
            return true;
        }

        if (AutorankTools.isExcludedFromRanking(targetPlayer)) {
            sender.sendMessage(Lang.PLAYER_IS_EXCLUDED.getConfigValue(targetPlayer.getName()));
            return true;
        }

        // Check the player
        plugin.getPlayerChecker().checkPlayer(targetPlayer);

        // Let checker know that we checked.
        sender.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " checked!");

        return true;
    }

    @Override
    public String getDescription() {
        return "Do a manual silent check.";
    }

    @Override
    public String getPermission() {
        return AutorankPermission.FORCE_CHECK;
    }

    @Override
    public String getUsage() {
        return "/ar forcecheck <player>";
    }
}
