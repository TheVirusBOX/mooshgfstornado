package com.moosh.tornados;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Drivable armored storm-chaser vehicle. Right-click to board (up to 4 seats), WASD to drive. */
public class DominatorEntity extends Mob {
    private static final double[][] SEATS = {{0.45, 0.35}, {-0.45, 0.35}, {0.45, -0.75}, {-0.45, -0.75}};
    private static final int[] RESIST = {2, 3, 3, 4};   // strongest F-scale it can ignore
    private final int level;

    public DominatorEntity(EntityType<? extends Mob> type, Level world, int level) {
        super(type, world);
        this.level = level;
        this.setPersistenceRequired();
        this.setMaxUpStep(1.0f);
    }

    public int resistance() { return RESIST[level - 1]; }

    public static AttributeSupplier.Builder attributes(int level) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150 + 50 * level)
                .add(Attributes.ARMOR, 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30 + 0.05 * level);
    }

    @Override protected void registerGoals() { }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean canBeLeashed(Player p) { return false; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive() && getPassengers().size() < SEATS.length) {
            if (!this.level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override protected boolean canAddPassenger(Entity e) { return getPassengers().size() < SEATS.length; }
    @Override public LivingEntity getControllingPassenger() { return getFirstPassenger() instanceof LivingEntity le ? le : null; }

    @Override
    protected void positionRider(Entity p, Entity.MoveFunction cb) {
        int idx = Math.max(0, getPassengers().indexOf(p));
        double yaw = Math.toRadians(getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw);
        double[] s = SEATS[Math.min(idx, SEATS.length - 1)];
        cb.accept(p, getX() + fx * s[1] * 1.0 + lx * s[0], getY() + 0.55, getZ() + fz * s[1] * 1.0 + lz * s[0]);
    }

    @Override
    public void travel(Vec3 v) {
        LivingEntity rider = getControllingPassenger();
        if (this.isAlive() && this.isVehicle() && rider != null) {
            setYRot(rider.getYRot()); yRotO = getYRot(); setXRot(0); setRot(getYRot(), getXRot());
            yBodyRot = yHeadRot = getYRot();
            float strafe = rider.xxa * 0.35f, fwd = rider.zza;
            if (fwd < 0) fwd *= 0.5f;
            if (isControlledByLocalInstance()) {
                setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6f);
                super.travel(new Vec3(strafe, v.y, fwd));
            } else setDeltaMovement(Vec3.ZERO);
        } else super.travel(v);
    }
}
