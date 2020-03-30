@file:JvmName("MobzyAPI")

package com.mineinabyss.mobzy

import com.mineinabyss.idofront.messaging.logInfo
import com.mineinabyss.mobzy.mobs.CustomMob
import com.mineinabyss.mobzy.mobs.MobTemplate
import net.minecraft.server.v1_15_R1.EntityTypes
import net.minecraft.server.v1_15_R1.EnumCreatureType
import net.minecraft.server.v1_15_R1.World
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.io.File
import net.minecraft.server.v1_15_R1.Entity as EntityNMS

//====================================================================
// LOGGING AND DEBUGGING
//====================================================================

fun <T> T.debugVal(message: String = ""): T = debug("$message $this").let { this }

/**
 * Broadcast a message if the debug option is enabled in config
 *
 * @param message the message to be sent
 */
fun debug(message: String, colorChar: Char? = null) {
    if (MobzyConfig.isDebug) logInfo(message, colorChar)
}

//====================================================================
// ENTITY EXTENSION FUNCTIONS
//====================================================================

/**
 * Whether an entity is a renamed mob registered with Mobzy.
 */
val Entity.isRenamed
    get() = if (toNMS().isCustomMob || customName == null) false else customName != this.mobName

/**
 * The mobzy ID for a registered custom mob.
 */
val Entity.mobName get() = toNMS().mobName

/**
 * The mobzy ID for a registered custom mob.
 */
val EntityNMS.mobName get() = this.entityType.mobName

/**
 * Converts [Entity] to [CustomMob].
 */
fun Entity.toMobzy() = toNMS().toMobzy()

/**
 * Converts [EntityNMS] to [CustomMob].
 */
fun EntityNMS.toMobzy() = this as CustomMob

/**
 * @return Whether the mob is of type of the given mob ID.
 */
fun Entity.isOfType(mobID: String) = toNMS().isOfType(mobID)

/**
 * @return Whether the mob is of type of the given mob ID.
 */
fun EntityNMS.isOfType(mobID: String) = this.mobName == mobID

/**
 * The [EnumCreatureType] for this [EntityTypes] object.
 */
val EntityTypes<*>.creatureType: EnumCreatureType get() = this.e()

/**
 * The name of the [EnumCreatureType] of this entity.
 */
val EntityNMS.creatureType
    get() = this.entityType.creatureType.name

fun Entity.isOfCreatureType(creatureType: String) = toNMS().isOfCreatureType(creatureType)

fun EntityNMS.isOfCreatureType(creatureType: String) = this.entityType.creatureType.name == creatureType

/**
 * @return whether this is a custom mob registered with Mobzy.
 */
val Entity.isCustomMob get() = toNMS().isCustomMob

/**
 * @return whether this is a custom mob registered with Mobzy.
 */
val EntityNMS.isCustomMob get() = this is CustomMob

/**
 * A custom mob's [MobTemplate] that is registered with Mobzy.
 */
val Entity.template: MobTemplate get() = toNMS().template

/**
 * A custom mob's [MobTemplate] that is registered with Mobzy.
 */
val EntityNMS.template: MobTemplate get() = (this as CustomMob).template

/**
 * A list of all the [CustomMob]s in these chunks.
 */
val List<Chunk>.customMobs get() = flatMap { it.customMobs }

/**
 * A list of all the [CustomMob]s in this chunk.
 */
val Chunk.customMobs get() = entities.filter { it.isCustomMob }

//====================================================================
// CUSTOM TYPE RELATED FUNCTIONS
//====================================================================

private val types: Map<String, EntityTypes<*>> get() = mobzy.mobzyTypes.types
private val templates: Map<String, MobTemplate> get() = mobzy.mobzyTypes.templates

fun String.toEntityTypeID() = toLowerCase().replace(" ", "_")
fun String.toEntityType(): EntityTypes<*> = types[toEntityTypeID()] ?: error("Mob type $this not found")
fun Set<String>.toEntityType() = types[first { types.containsKey(it.toEntityTypeID()) }.toEntityTypeID()]
        ?: error("No type found for $this. Registered types: $types")

/**
 * The name of the mob type.
 */
val EntityTypes<*>.name: String get() = this.f()

/**
 * The mob's [EntityTypes.name] without the `entity.minecraft.` prefix
 */
val EntityTypes<*>.mobName: String get() = this.name.removePrefix("entity.minecraft.")

val EntityTypes<*>.mobTemplate get() = mobName.toTemplate()
fun String.toTemplate() = templates[toEntityTypeID()] ?: error("Template for $this not found")
val MobTemplate.type get() = types[name.toEntityTypeID()] ?: error("No entity type found for template $this")

fun Location.spawnEntity(name: String) = spawnEntity(name.toEntityType())
fun Location.spawnEntity(type: EntityTypes<*>) = mobzy.mobzyTypes.spawnEntity(type, this)


fun registerHardCodedTemplate(mob: String, template: MobTemplate): MobTemplate =
        mobzy.mobzyTypes.registerHardCodedTemplate(mob, template).let { template }

fun registerEntity(name: String,
                   type: EnumCreatureType,
                   width: Float = 1f,
                   height: Float = 2f,
                   func: (World) -> EntityNMS) =
        mobzy.mobzyTypes.registerEntity(name, type, width, height, func)

fun registerTypes() = mobzy.mobzyTypes.registerTypes()

//====================================================================
// CONFIG REGISTRY FUNCTIONS
//====================================================================

/**
 * Registers a separate plugin's the spawn configuration file with the API
 *
 * @param configuration the file in which to look for a configuration
 * @param plugin        the plugin this configuration file corresponds to
 */
fun registerSpawnConfig(configuration: File, plugin: Plugin) =
        mobzy.mobzyConfig.registerSpawnCfg(configuration, plugin)

/**
 * Registers a separate plugin's the mob configuration file with the API (to read mob attributes)
 *
 * @param configuration the file in which to look for a configuration
 * @param plugin        the plugin this configuration file corresponds to
 */
fun registerMobConfig(configuration: File, plugin: Plugin) =
        mobzy.mobzyConfig.registerMobCfg(configuration, plugin)