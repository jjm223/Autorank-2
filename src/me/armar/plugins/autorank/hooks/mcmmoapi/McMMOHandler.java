package me.armar.plugins.autorank.hooks.mcmmoapi;

import org.bukkit.plugin.Plugin;

import com.gmail.nossr50.mcMMO;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.hooks.DependencyHandler;

/**
 * Handles all connections with McMMO.
 * <p>
 * Date created: 17:50:11 4 mrt. 2014
 * 
 * @author Staartvin
 * 
 */
public class McMMOHandler implements DependencyHandler {

	private Autorank plugin;
	private mcMMO api;

	public McMMOHandler(Autorank instance) {
		plugin = instance;
	}

	public Plugin get() {
		Plugin plugin = this.plugin.getServer().getPluginManager()
				.getPlugin("mcMMO");

		// WorldGuard may not be loaded
		if (plugin == null || !(plugin instanceof mcMMO)) {
			return null; // Maybe you want throw an exception instead
		}

		return plugin;
	}

	/* (non-Javadoc)
	 * @see me.armar.plugins.autorank.hooks.DependencyHandler#setup()
	 */
	@Override
	public boolean setup() {
		if (!isInstalled()) {
			plugin.getLogger().info("mcMMO has not been found!");
			return false;
		} else {
			api = (mcMMO) get();

			if (api != null) {
				plugin.getLogger()
						.info("mcMMO has been found and can be used!");
				return true;
			} else {
				plugin.getLogger().info(
						"mcMMO has been found but cannot be used!");
				return false;
			}
		}
	}

	/* (non-Javadoc)
	 * @see me.armar.plugins.autorank.hooks.DependencyHandler#isInstalled()
	 */
	@Override
	public boolean isInstalled() {
		mcMMO plugin = (mcMMO) get();

		return plugin != null && plugin.isEnabled();
	}

	/* (non-Javadoc)
	 * @see me.armar.plugins.autorank.hooks.DependencyHandler#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return api != null;
	}

}