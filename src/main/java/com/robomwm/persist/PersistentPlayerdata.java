package com.robomwm.persist;

import com.robomwm.usefulutil.UsefulUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.Warning;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created on 3/13/2019
 *
 * @author RoboMWM
 *
 * Plugin developers - there's no need to depend on this plugin to use it.
 * The persistent metadata resides in a map under the metadata key "PERSIST"
 * {@code Map<String, Object> data = player#getMetadata("PERSIST")}
 *
 * Metadata is loaded on first join since most recent server startup.
 * Metatdata is saved every time the player leaves.
 */
public class PersistentPlayerdata implements Listener
{
    private Plugin plugin;
    private final String METADATA_KEY = "PERSIST";
    private long autoSaveInterval;
    private Map<UUID, YamlConfiguration> cachedYamls = new HashMap<>();

    public PersistentPlayerdata(Plugin plugin)
    {
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection playerdataSection = config.getConfigurationSection("playerdata");
        if (playerdataSection == null)
            playerdataSection = config.createSection("playerdata");

        playerdataSection.addDefault("autoSaveInterval", 0);

        config.options().copyDefaults();

        autoSaveInterval = playerdataSection.getInt("autoSaveInterval");

        //cache data for players that exist in Usercache.json
        for (OfflinePlayer player : plugin.getServer().getOfflinePlayers())
        {
            File file = new File(plugin.getDataFolder() + File.separator + player.getUniqueId().toString() + ".yml");
            cachedYamls.put(player.getUniqueId(), YamlConfiguration.loadConfiguration(file));
        }

        //in case of plugin disable then re-enable with different sessions
        for (Player player : plugin.getServer().getOnlinePlayers())
        {
            loadPlayerdata(player);
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (autoSaveInterval < 1)
            return;
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                for (Player player : plugin.getServer().getOnlinePlayers())
                {
                    savePlayerdata(player);
                }
            }
        }.runTaskTimer(plugin, autoSaveInterval, autoSaveInterval);
    }

    /**
     * Returns a copy of the data as stored in the plugin
     * @param uuid
     * @return a copy of the data in a map. Can be outdated if player is online.
     */
    @Warning(reason = "Will be inconsistent if player is online, get and use metadata instead")
    public Map<String, Object> getPlayerData(UUID uuid)
    {
        File file = new File(plugin.getDataFolder() + File.separator + uuid.toString() + ".yml");
        YamlConfiguration yaml = cachedYamls.getOrDefault(uuid, YamlConfiguration.loadConfiguration(file));
        Map<String, Object> data = new HashMap<>();
        for (String key : yaml.getKeys(false))
            data.put(key, yaml.get(key));
        return data;
    }

    public Player loadPlayerdata(Player player)
    {
        if (!player.hasMetadata(METADATA_KEY))
            return player;

        player.setMetadata(METADATA_KEY,
                new FixedMetadataValue(plugin, getPlayerData(player.getUniqueId())));

        return player;
    }

    @SuppressWarnings("unchecked cast")
    public boolean savePlayerdata(Player player)
    {
        if (!player.hasMetadata(METADATA_KEY))
            return false;

        String uuidString = player.getUniqueId().toString();
        YamlConfiguration yaml = new YamlConfiguration();
        Map<String, Object> data = (Map<String, Object>)player.getMetadata(METADATA_KEY).get(0).value();
        for (Map.Entry<String, Object> entry : data.entrySet())
        {
            //put checks in here later, maybe
            yaml.set(entry.getKey(), entry.getValue());
        }

        File file = new File(plugin.getDataFolder() + File.separator + uuidString + ".yml");
        UsefulUtil.saveStringToFile(plugin, file, yaml.saveToString());

        return true;
    }

    public void onDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers())
            savePlayerdata(player);
        //May need to block thread to ensure save threads finish?
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerJoin(PlayerJoinEvent event)
    {
        loadPlayerdata(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerLeave(PlayerQuitEvent event)
    {
        savePlayerdata(event.getPlayer());
    }
}
