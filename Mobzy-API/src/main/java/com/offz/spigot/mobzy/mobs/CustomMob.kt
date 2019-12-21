package com.offz.spigot.mobzy.mobs

import com.offz.spigot.mobzy.CustomType
import com.offz.spigot.mobzy.debug
import com.offz.spigot.mobzy.pathfinders.Navigation
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import net.minecraft.server.v1_15_R1.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld
import org.bukkit.craftbukkit.v1_15_R1.event.CraftEventFactory
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

/**
 * @property killScore The score with which a player should be rewarded with when the current entity is killed.
 * @property killer The killer of the current entity if it has one.
 */
interface CustomMob {
    // ========== Useful properties ===============
    val entity: EntityLiving
    val living: LivingEntity get() = entity.bukkitEntity as LivingEntity
    val template: MobTemplate
    val staticTemplate: MobTemplate get() = CustomType.getTemplate(entity.entityType)
    val x: Double get() = living.location.x
    val y: Double get() = living.location.y
    val z: Double get() = living.location.z
    private val world: World get() = (living.world as CraftWorld).handle
    private val location: Location get() = living.location
    val navigation: Navigation
        get() = Navigation((entity as EntityInsentient).navigation, entity as EntityInsentient, staticTemplate.movementSpeed ?: 0.7)
    val killer: EntityLiving? get() = entity.killer
    fun expToDrop(): Int {
        return if (template.minExp == null || template.maxExp == null) entity.expToDrop
        else if(template.maxExp!! <= template.minExp!!) template.minExp!!
        else Random.nextInt(template.minExp!!, template.maxExp!!)
    }

    // ========== Things to be overloaded ==========
    val soundAmbient: String?
        get() = null
    val soundHurt: String?
        get() = null
    val soundDeath: String?
        get() = null
    val soundStep: String?
        get() = null
    var killedMZ: Boolean
    val killScore: Int

    fun createPathfinders()
    fun lastDamageByPlayerTime(): Int
    fun saveMobNBT(nbttagcompound: NBTTagCompound?)
    fun loadMobNBT(nbttagcompound: NBTTagCompound?)

    fun onRightClick(player: EntityHuman) {}

    // ========== Pre-written behaviour ============

    /**
     * Applies some default attributes that every custom mob should have, such as a model, invisibility, and an
     * identifier scoreboard tag
     */
    fun createFromBase() {
        entity.expToDrop = 3
        entity.addScoreboardTag("customMob2")
        entity.addScoreboardTag(template.name)

        //create an item based on model ID in head slot if entity will be using itself for the model
        living.equipment!!.helmet = template.modelItemStack

        //disguise the entity
        DisguiseAPI.disguiseEntity(entity.bukkitEntity, MobDisguise(template.disguiseAs, template.isAdult).also { it.watcher.isInvisible = true })
    }

    fun setConfiguredAttributes() {
        if (staticTemplate.maxHealth != null)
            entity.getAttributeInstance(GenericAttributes.MAX_HEALTH).value = staticTemplate.maxHealth!!
        if (staticTemplate.movementSpeed != null)
            entity.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).value = staticTemplate.movementSpeed!!
        if (staticTemplate.attackDamage != null /*&& this !is FlyingMob TODO add this back*/) //flying mobs can't have an attack damage attribute, we use the builder's value instead
            entity.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).value = staticTemplate.attackDamage!!
        if (staticTemplate.followRange != null)
            entity.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).value = staticTemplate.followRange!!
    }

    fun dieCM(damageSource: DamageSource?) {
        if (!killedMZ) {
            killedMZ = true
            debug("${ChatColor.RED}${template.name} died at coords ${x.toInt()} ${y.toInt()} ${z.toInt()}")
            if (killScore >= 0 && killer != null) killer!!.a(entity, killScore, damageSource)
            // this line causes the entity to send a statistics update on death (we don't want this as it causes a NPE exception and crash)
//            if (entity != null) entity.b(this);

            if (entity.isSleeping) entity.entityWakeup()

            if (!entity.world.isClientSide) {
                if (world.gameRules.getBoolean(GameRules.DO_MOB_LOOT)) {
                    CraftEventFactory.callEntityDeathEvent(entity, template.chooseDrops())
                    entity.expToDrop = expToDrop()
                    dropExp()
                } else CraftEventFactory.callEntityDeathEvent(entity)
            }

            world.broadcastEntityEffect(entity, 3.toByte())
//            entity.setPose(EntityPose.DYING)
            //TODO add PlaceHolderAPI support
            template.deathCommands.forEach { Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), it) }
        }
    }

    fun dropExp()

    fun makeSound(sound: String?) {
        if (sound != null)
            living.world.playSound(location, sound, SoundCategory.NEUTRAL, 1f, (kotlin.random.Random.nextDouble(1.0, 1.02).toFloat()))
    }

    // ========== Helper methods ===================
    fun addPathfinderGoal(priority: Int, goal: PathfinderGoal) {
        (entity as EntityInsentient).goalSelector.a(priority, goal)
    }

    fun randomSound(vararg sounds: String?): String? = sounds[Random.nextInt(sounds.size)]

    /**
     * @param other Another entity.
     * @return The distance between the current entity and other entity's locations.
     */
    fun distanceToEntity(other: Entity): Double {
        return location.distance(other.location)
    }

    /**
     * @param range the range to search within
     * @return a nearby player, or null if none are in the range
     */
    fun findNearbyPlayer(range: Double) = world.findNearbyPlayer(this.entity, range)

    fun lookAt(x: Double, y: Double, z: Double) =
            (entity as EntityInsentient).controllerLook.a(x, y, z)

    /**
     * @param location the location to look at
     *
     * Be careful and ensure that the custom mob using this is an [EntityInsentient]
     */
    fun lookAt(location: Location) = lookAt(location.x, location.y, location.z)

    /**
     * @param entity the entity to look at
     *
     * Be careful and ensure that the custom mob using this is an [EntityInsentient]
     */
    fun lookAt(entity: Entity) = lookAt(entity.location)

    fun lookAtPitchLock(location: Location) = lookAt(location.x, y + entity.headHeight, location.z)

    fun lookAtPitchLock(entity: Entity) = lookAtPitchLock(entity.location)

//    fun jump() = (entity as EntityInsentient).controllerJump.jump()
}