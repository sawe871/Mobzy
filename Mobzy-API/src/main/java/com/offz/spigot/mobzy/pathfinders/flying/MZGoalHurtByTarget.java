package com.offz.spigot.mobzy.pathfinders.flying;

import net.minecraft.server.v1_15_R1.EntityHuman;
import net.minecraft.server.v1_15_R1.EntityInsentient;
import net.minecraft.server.v1_15_R1.EntityLiving;
import net.minecraft.server.v1_15_R1.PathfinderGoal;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

public class MZGoalHurtByTarget extends PathfinderGoal {
    private EntityInsentient entity;

    public MZGoalHurtByTarget(EntityInsentient entity) {
        this.entity = entity;
    }

    public boolean a() {
        EntityLiving damager = entity.getLastDamager();
        if (damager instanceof EntityHuman && ((EntityHuman) damager).abilities.isInvulnerable)
            return false;
        return damager != null;
    }

    public void c() {
        entity.setGoalTarget(entity.getLastDamager(), TargetReason.TARGET_ATTACKED_NEARBY_ENTITY, true);
    }

    public void d(){
        entity.setGoalTarget(null);
    }
}