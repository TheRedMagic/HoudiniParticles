package com.redmagic.template

import com.redmagic.undefinedapi.UndefinedAPI
import org.bukkit.plugin.java.JavaPlugin

class Template : JavaPlugin() {

    val undefinedAPI = UndefinedAPI(this)

    override fun onEnable() {



    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
