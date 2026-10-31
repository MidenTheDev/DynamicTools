package us.to.midensthings.dynamicTools.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.to.midensthings.dynamicTools.DynamicTools;

/***
 * Runs tasks on the right thread on both Paper and Folia. Folia has no main thread: players are owned by the thread of
 * the region they're in, and everything else runs on the global region thread. On Paper all of these run on the main thread.
 * The Bukkit scheduler doesn't work on Folia, so use these instead.
 */
public final class Schedulers {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private Schedulers() {
    }

    /***
     * Runs the task on the thread that owns the player, right away if this is already that thread.
     * Does nothing if the player logs out before it runs.
     */
    public static void runFor(Player player, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            task.run();
        } else {
            runNextTick(player, task);
        }
    }

    /***
     * Runs the task on the player's thread next tick, e.g. to open an inventory after a click has finished.
     */
    public static void runNextTick(Player player, Runnable task) {
        player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    /***
     * Runs the task on the thread that should talk to the sender: the player's thread for players, the global thread otherwise.
     */
    public static void runForSender(CommandSender sender, Runnable task) {
        if (sender instanceof Player player) {
            runNextTick(player, task);
        } else {
            runGlobal(task);
        }
    }

    /***
     * Runs the task on the global region thread, which is where console commands run.
     */
    public static void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    /***
     * Runs the task off the server threads. It mustn't touch worlds, entities or players.
     */
    public static void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }
}
