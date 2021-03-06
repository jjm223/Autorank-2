package me.armar.plugins.autorank.playerchecker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.pathbuilder.Path;
import me.armar.plugins.autorank.pathbuilder.holders.RequirementsHolder;
import me.armar.plugins.autorank.pathbuilder.result.Result;
import me.armar.plugins.autorank.permissions.AutorankPermission;
import me.armar.plugins.autorank.util.AutorankTools;

/*
 * PlayerChecker is where the magic happens :P It has a RankChangeBuilder that reads
 * the config and makes new RankChange objects. It sends the names of the needed results
 * and requirements to AdditionalRequirementBuilder and ResultBuilder. Those are dynamic
 * factories because they don't have any hardcoded classes to build. You register all
 * the requirements or results when the plugin is started. Because of this other
 * plugins / addons can register their own custom requirements and results very easily.
 * 
 * So: PlayerChecker has a list of RankChanges and a RankChange has a list of AdditionalRequirement and Results.
 * 
 */
public class PlayerChecker {

    private final Autorank plugin;

    public PlayerChecker(final Autorank plugin) {
        this.plugin = plugin;
    }

    public boolean checkPlayer(final Player player) {

        // Do not rank a player when he is excluded
        if (AutorankTools.isExcludedFromRanking(player))
            return false;

        // Get chosen path
        Path chosenPath = plugin.getPathManager().getCurrentPath(player.getUniqueId());

        if (chosenPath == null)
            return false;

        return chosenPath.applyChange(player);
    }

    public void doLeaderboardExemptCheck(final Player player) {
        plugin.getPlayerDataConfig().hasLeaderboardExemption(player.getUniqueId(),
                player.hasPermission(AutorankPermission.EXCLUDE_FROM_LEADERBOARD));
    }

    public List<String> formatRequirementsToList(final List<RequirementsHolder> holders,
            final List<RequirementsHolder> metRequirements) {
        // Converts requirements into a list of readable requirements

        final List<String> messages = new ArrayList<String>();

        messages.add(ChatColor.GRAY + " ------------ ");

        for (int i = 0; i < holders.size(); i++) {
            final RequirementsHolder holder = holders.get(i);

            if (holder != null) {
                final StringBuilder message = new StringBuilder("     " + ChatColor.GOLD + (i + 1) + ". ");
                if (metRequirements.contains(holder)) {
                    message.append(ChatColor.RED + holder.getDescription() + ChatColor.BLUE + " ("
                            + Lang.DONE_MARKER.getConfigValue() + ")");
                } else {
                    message.append(ChatColor.RED + holder.getDescription());
                }

                if (holder.isOptional()) {
                    message.append(ChatColor.AQUA + " (" + Lang.OPTIONAL_MARKER.getConfigValue() + ")");
                }

                messages.add(message.toString());

            }
        }

        return messages;

    }

    public List<String> formatResultsToList(List<Result> results) {
        // Converts requirements into a list of readable requirements

        final List<String> messages = new ArrayList<String>();

        messages.add(ChatColor.GRAY + " ------------ ");

        for (int i = 0; i < results.size(); i++) {
            final Result result = results.get(i);

            if (result != null) {
                final StringBuilder message = new StringBuilder("     " + ChatColor.GOLD + (i + 1) + ". ");

                message.append(ChatColor.RED + result.getDescription());

                messages.add(message.toString());

            }
        }

        return messages;

    }

    /**
     * Get a list of Requirements that the player needs to complete for its current path. Returns an empty list if
     * the player has not chosen a path yet.
     * @param player Player to check the path of.
     * @return A list of RequirementsHolders that ought to be completed before the path is completed.
     */
    public List<RequirementsHolder> getAllRequirementsHolders(final Player player) {
        // Get chosen path
        Path chosenPath = plugin.getPathManager().getCurrentPath(player.getUniqueId());

        if (chosenPath != null) {
            return chosenPath.getRequirements();
        } else {
            return new ArrayList<RequirementsHolder>();
        }
    }

    /**
     * Get a list of Requirements that the player did not pass (yet). Returns an empty list if the player has not chosen
     * any path yet.
     * @param player Player to check path for.
     * @return a list of RequirementsHolders that the player did not complete yet.
     */
    public List<RequirementsHolder> getFailedRequirementsHolders(final Player player) {

        List<RequirementsHolder> holders = new ArrayList<>();

        // Get chosen path
        Path chosenPath = plugin.getPathManager().getCurrentPath(player.getUniqueId());

        if (chosenPath != null) {
            holders.addAll(chosenPath.getFailedRequirements(player));
        }

        return holders;
    }

    /**
     * Get all requirements that a player has completed in its path.
     * @param player Player to check
     * @return List of RequirementsHolders that the player completed.
     */
    public List<RequirementsHolder> getCompletedRequirementsHolders(Player player) {
       return this.getMetRequirementsHolders(this.getAllRequirementsHolders(player), player);

    }

    /**
     * Get a list of Requirements that the player completed, given a set of Requirements.
     * The {@link #getCompletedRequirementsHolders(Player)} uses this method with the requirements of the player's
     * current path.
     * @param holders A list of holders to check.
     * @param player Player to check holders for.
     * @return a subset of the given list of holders that the player completed.
     */
    public List<RequirementsHolder> getMetRequirementsHolders(final List<RequirementsHolder> holders, final Player player) {
        final List<RequirementsHolder> metRequirements = new ArrayList<>();

        boolean onlyOptional = true;

        // Check if we only have optional requirements
        for (final RequirementsHolder holder : holders) {
            if (!holder.isOptional())
                onlyOptional = false;
        }

        if (onlyOptional) {

            for (final RequirementsHolder holder : holders) {
                metRequirements.add(holder);
            }

            return metRequirements;
        }

        for (final RequirementsHolder holder : holders) {
            final int reqID = holder.getReqID();

            // Use auto completion
            if (holder.useAutoCompletion()) {
                // Do auto complete
                if (holder.meetsRequirement(player, false)) {
                    // Player meets the requirement -> give him results

                    // Doesn't need to check whether this requirement was
                    // already done
                    if (!plugin.getConfigHandler().usePartialCompletion())
                        continue;

                    metRequirements.add(holder);
                    continue;
                } else {

                    // Only check if player has done this when partial
                    // completion is used
                    if (plugin.getConfigHandler().usePartialCompletion()) {
                        // Player does not meet requirements, but has done this
                        // already
                        if (plugin.getPlayerDataConfig().hasCompletedRequirement(reqID, player.getUniqueId())) {
                            metRequirements.add(holder);
                            continue;
                        }
                    }

                    // If requirement is optional, we do not check.
                    if (holder.isOptional()) {
                        continue;
                    }

                    // Player does not meet requirements -> do nothing
                    continue;
                }
            } else {

                if (!plugin.getConfigHandler().usePartialCompletion()) {

                    // Doesn't auto complete and doesn't meet requirement, then
                    // continue searching
                    if (!holder.meetsRequirement(player, false)) {

                        // If requirement is optional, we do not check.
                        if (holder.isOptional()) {
                            continue;
                        }

                        continue;
                    } else {
                        // Player does meet requirement, continue searching
                        continue;
                    }

                }

                // Do not auto complete
                if (plugin.getPlayerDataConfig().hasCompletedRequirement(reqID, player.getUniqueId())) {
                    // Player has completed requirement already
                    metRequirements.add(holder);
                    continue;
                } else {

                    // If requirement is optional, we do not check.
                    if (holder.isOptional()) {
                        continue;
                    }

                    continue;
                }
            }
        }

        return metRequirements;
    }
}