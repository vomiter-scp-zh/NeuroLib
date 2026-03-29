package com.vomiter.neurolib.common.entity.gather.eat;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

public final class MobEatingFx {

    private ItemStack eatingStack = ItemStack.EMPTY;
    private int ticksLeft;
    private final int durationTicks;

    public MobEatingFx(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    public boolean isEating() {
        return ticksLeft > 0;
    }

    public void start(ItemStack stack) {
        this.eatingStack = stack;
        this.ticksLeft = durationTicks;
    }

    public void tick(PathfinderMob mob) {
        if (ticksLeft <= 0) {
            eatingStack = ItemStack.EMPTY;
            return;
        }

        ticksLeft--;
        mob.getNavigation().stop();

        if ((ticksLeft % 3) == 0 && !eatingStack.isEmpty()) {
            spawnItemParticles(mob, eatingStack);
        }
    }

    public static void playDefaultBiteSounds(PathfinderMob mob) {
        playBiteSounds(mob, SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F, null, 0.0F, 0.0F);
    }

    public static void playBiteSounds(
            PathfinderMob mob,
            SoundEvent primary, float primaryVolume, float primaryPitch,
            SoundEvent secondary, float secondaryVolume, float secondaryPitch
    ) {
        mob.playSound(primary, primaryVolume, primaryPitch);
        if (secondary != null) {
            mob.playSound(secondary, secondaryVolume, secondaryPitch);
        }
    }

    private static void spawnItemParticles(PathfinderMob mob, ItemStack stack) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        var particle = new ItemParticleOption(ParticleTypes.ITEM, stack.getItem());
        double x = mob.getX();
        double y = mob.getY() + mob.getEyeHeight() * 0.6;
        double z = mob.getZ();

        serverLevel.sendParticles(particle, x, y, z, 5, 0.2, 0.2, 0.2, 0.05);
    }
}