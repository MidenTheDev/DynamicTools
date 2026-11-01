package us.to.midensthings.dynamicTools.compat;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.util.Schedulers;

/***
 * Reloads CraftEngine once the server has finished starting if it started before DynamicTools added its files,
 * so the new blocks, configs and textures are loaded without a manual /ce reload all.
 */
public class CraftEngineStartupReload implements Listener {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    @EventHandler
    public void onServerLoad(ServerLoadEvent e) {
        if (!plugin.takeSourceReload()) {
            return;
        }
        Schedulers.runGlobal(() -> {
            plugin.getLogger().info("Reloading CraftEngine to load the files DynamicTools added");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ce reload all");
        });
    }
}
