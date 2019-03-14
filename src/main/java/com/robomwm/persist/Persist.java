package com.robomwm.persist;

import org.bukkit.plugin.java.JavaPlugin;

public class Persist extends JavaPlugin
{
    private PersistentPlayerdata persistentPlayerdata;

    @Override
    public void onEnable()
    {
        persistentPlayerdata = new PersistentPlayerdata(this);
    }

    public PersistentPlayerdata getPersistentPlayerdata()
    {
        return persistentPlayerdata;
    }

    @Override
    public void onDisable()
    {
        persistentPlayerdata.onDisable();
    }
}
