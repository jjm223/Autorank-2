package me.armar.plugins.autorank.permissions.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.permissions.PermissionsHandler;

/**
 * @author Staartvin This is a special permission handler that handles all work
 *         from PermissionsBukkit
 */
public class PermissionsBukkitHandler implements PermissionsHandler {

    private PermissionsPlugin permissionsBukkit;
    private final Autorank plugin;

    public PermissionsBukkitHandler(final Autorank plugin) {
        this.plugin = plugin;
        setupPermissionsBukkit();
    }

    /**
     * Add a player to group
     * 
     * @param player
     *            Player to add
     * @param world
     *            On a specific world
     * @param group
     *            Group to add the player to
     * @return true if done, false if failed
     */
    public boolean addGroup(final Player player, final String world, final String group) {
        // PermissionsBukkit doesn't have a method to set the actual group.
        // Therefore we need to do it with commands...
        // Come on PermBukkit. Fix your API..
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                "permissions player addgroup " + player.getName() + " " + group);
        return true;
        // There is no way to check if the command was successful.
    }

    @Override
    public boolean demotePlayer(final Player player, final String world, final String groupFrom, final String groupTo) {
        return (addGroup(player, world, groupTo) && removeGroup(player, world, groupFrom));
    }

    /**
     * Get all known groups
     * 
     * @return an array of strings containing all setup groups of the
     *         permissions plugin.
     */
    @Override
    public Collection<String> getGroups() {
        final List<Group> groups = permissionsBukkit.getAllGroups();
        List<String> groupNames = new ArrayList<>();

        for (Group group : groups) {
            groupNames.add(group.getName());
        }

        return Collections.unmodifiableCollection(groupNames);
    }

    /*
     * (non-Javadoc)
     * 
     * @see me.armar.plugins.autorank.permissions.PermissionsHandler#getName()
     */
    @Override
    public String getName() {
        return "PermissionsBukkit";
    }

    @Override
    public Collection<String> getPlayerGroups(final Player player) {
        @SuppressWarnings("deprecation")
        final List<Group> groups = permissionsBukkit.getGroups(player.getName());
        List<String> groupNames = new ArrayList<>();

        for (Group group : groups) {
            groupNames.add(group.getName());
        }

        return Collections.unmodifiableCollection(groupNames);
    }

    @Override
    public Collection<String> getWorldGroups(final Player player, final String world) {
        @SuppressWarnings("deprecation")
        final List<Group> groups = permissionsBukkit.getGroups(player.getName());
        List<String> groupNames = new ArrayList<>();

        for (Group group : groups) {
            groupNames.add(group.getName());
        }

        return Collections.unmodifiableCollection(groupNames);
    }

    /**
     * Remove a player from a group
     * 
     * @param player
     *            Player to remove
     * @param world
     *            On a specific world
     * @param group
     *            Group to remove the player from
     * @return true if done, false if failed
     */
    public boolean removeGroup(final Player player, final String world, final String group) {
        // PermissionsBukkit doesn't have a method to set the actual group.
        // Therefore we need to do it with commands...
        // Come on PermBukkit. Fix your API..
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                "permissions player removegroup " + player.getName() + " " + group);
        return true;
        // There is no way to check if the command was successful.
    }

    @Override
    public boolean replaceGroup(final Player player, final String world, final String oldGroup, final String newGroup) {
        return (addGroup(player, world, newGroup) && removeGroup(player, world, oldGroup));
    }

    private boolean setupPermissionsBukkit() {
        final PluginManager pluginManager = plugin.getServer().getPluginManager();
        final Plugin permBukkit = pluginManager.getPlugin("PermissionsBukkit");

        if (permBukkit != null && permBukkit.isEnabled()) {
            permissionsBukkit = (PermissionsPlugin) permBukkit;
        }

        return permissionsBukkit != null;
    }
}
