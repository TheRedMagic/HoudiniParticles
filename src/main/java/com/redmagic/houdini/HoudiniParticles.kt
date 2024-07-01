package com.redmagic.houdini

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.undefined.api.UndefinedAPI
import com.undefined.api.command.UndefinedCommand
import com.undefined.api.nms.createFakeEntity
import com.undefined.api.nms.interfaces.NMSBlockDisplayEntity
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.api.sendLog
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileReader
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt

class HoudiniParticles : JavaPlugin() {

    lateinit var undefinedAPI: UndefinedAPI

    var map: HashMap<Int, MutableList<SimulationData>> = hashMapOf()

    override fun onEnable() {

        undefinedAPI = UndefinedAPI(this)


        getData().thenAccept {
            map = it
        }

        UndefinedCommand("Test")
            .addExecutePlayer {

                val b = undefinedAPI.createFakeEntity(EntityType.BLOCK_DISPLAY, Material.STONE.createBlockData())!! as NMSBlockDisplayEntity

                b.addViewer(player!!)
                b.spawn(player!!.location.block.location)

                b.scale(-50f)

                return@addExecutePlayer true
            }

        UndefinedCommand("run")
            .addExecutePlayer {

                val start = this.location.block.location.clone()

                var tick = 1

                var h = HashMap<Int, NMSBlockDisplayEntity>()

                repeatingTask(1) {

                    val list = map.getOrElse(tick) {cancel(); return@repeatingTask}

                    list.forEach {

                        val entity = if (h.containsKey(it.simId)) h[it.simId]!! else {
                            val nms = undefinedAPI.createFakeEntity(EntityType.BLOCK_DISPLAY, Material.STONE.createBlockData())!! as NMSBlockDisplayEntity
                            nms.addViewer(player!!)
                            nms.spawn(start)
                            nms.scale(it.scale)
                            nms.viewRange = 5f
                            nms.blockData = it.blockData
                            nms
                        }

                        entity.transformationInterpolationDuration = 1

                        entity.offsetY = it.position[1].toFloat()
                        entity.offsetX = it.position[0].toFloat()
                        entity.offsetZ = it.position[2].toFloat()

                        entity.scale(it.scale)

                        if (entity.blockData != it.blockData) entity.blockData = it.blockData

                        if (!h.containsKey(it.simId)) h[it.simId] = entity
                    }

                    tick++

                }

                return@addExecutePlayer true
            }

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }


    fun getData(): CompletableFuture<HashMap<Int, MutableList<SimulationData>>> = CompletableFuture.supplyAsync {



        val color = File(dataFolder, "color.json")
        val hMap: HashMap<DoubleArray, BlockData> = hashMapOf()
        FileReader(color).use {
            val json = JsonParser.parseString(it.readText()).asJsonObject

            json.keySet().forEach {

                val j = json.getAsJsonObject(it)

                val r = j.get("red").asDouble
                val b = j.get("blue").asDouble
                val g = j.get("green").asDouble

                val array = DoubleArray(3)
                array[0] = r
                array[1] = b
                array[2] = g

                val m = Material.valueOf(it.uppercase())

                hMap[array] = m.createBlockData()
            }

        }

        val directory = File(dataFolder, "info")
        val jsonFiles = directory.listFiles { _, name -> name.endsWith(".json") }

        val map: HashMap<Int, MutableList<SimulationData>> = hashMapOf()

        sendLog("Started loading data")

        jsonFiles?.forEach { file ->
            val name = file.name.replace(".json", "").toDouble().toInt()
            FileReader(file).use { reader ->

                val text = reader.readText()

                val json = JsonParser.parseString(text).asJsonArray

                for (i in 0 until json.size()) {
                    val jsonObject: JsonObject = json.get(i).getAsJsonObject()

                    val id = jsonObject["id"].asInt
                    val position = jsonObject["position"].asJsonArray

                    val r = 50

                    val pList = listOf(position[0].asDouble * r , position[1].asDouble * r, position[2].asDouble * r)

                    val Cd = jsonObject["Cd"].asJsonArray

                    val rgbList = DoubleArray(3)
                    rgbList[0] = Cd[0].asDouble * 255
                    rgbList[1] = Cd[1].asDouble * 255
                    rgbList[2] = Cd[2].asDouble * 255

                    val block = getClosetBlock(hMap, rgbList)

                    var scale = jsonObject["pscale"].asDouble

                    map.getOrElse(name) { map[name] = mutableListOf() }.let {
                        val l = map[name]!!
                        l.add(SimulationData(id, pList, block, scale.toFloat()))
                        map[name] = l
                    }
                }
                sendLog("Loaded file: ${file.name}")
            }
        }

        sendLog("Done loading data. Size: ${map.size}")
        return@supplyAsync map

    }


    fun getClosetBlock(map: HashMap<DoubleArray, BlockData>, rgb: DoubleArray ): BlockData {
        var closestRGB: DoubleArray? = null
        var closestDistance = Double.MAX_VALUE

        map.forEach {
            val d = calculateDistance(it.key, rgb)
            if (d < closestDistance) {
                closestDistance = d
                closestRGB = it.key
            }
        }

        return map[closestRGB]!!
    }

    private fun calculateDistance(rgb1: DoubleArray, rgb2: DoubleArray): Double {
        val redDiff = rgb1[0] - rgb2[0]
        val greenDiff = rgb1[1] - rgb2[1]
        val blueDiff = rgb1[2] - rgb2[2]
        return sqrt((redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff))
    }

}

data class SimulationData(
    val simId: Int,
    val position: List<Double>,
    val blockData: BlockData,
    val scale: Float
)