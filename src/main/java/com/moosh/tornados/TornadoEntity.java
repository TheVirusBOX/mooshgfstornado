package com.moosh.tornados;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/** An F1-F5 tornado. Server-side: moves, flings entities, rips blocks. Client only needs the synced F value. */
public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Integer> F = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    public static final double[] RADIUS = {5, 9, 15, 24, 38};
    public static final double[] HEIGHT = {30, 50, 80, 120, 160};
    private static final double[] SPEED = {0.06, 0.09, 0.12, 0.15, 0.18};
    private static final float[] DAMAGE = {2, 4, 7, 11, 18};
    private static final float[] HARDNESS = {0.6f, 2.0f, 4.0f, 10.0f, 50.0f};
    private static final int[] BLOCKS_PER_TICK = {1, 2, 3, 5, 8};

    private int age;
    private double heading = Math.random() * Math.PI * 2;

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public int getF() { return this.entityData.get(F); }
    public void setF(int f) { this.entityData.set(F, Math.max(1, Math.min(5, f))); }

    @Override protected void defineSynchedData() { this.entityData.define(F, 1); }
    @Override protected void readAdditionalSaveData(CompoundTag t) { setF(t.getInt("F")); age = t.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { t.putInt("F", getF()); t.putInt("Age", age); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return true; }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel sl)) return;
        age++;
        int f = getF(), i = f - 1;
        double R = RADIUS[i], H = HEIGHT[i];
        if (age > 2400 + 1200 * f) { discard(); return; }

        // ---- movement ----
        if (age % 40 == 0) heading += (this.random.nextDouble() - 0.5) * 1.2;
        double nx = getX() + Math.cos(heading) * SPEED[i], nz = getZ() + Math.sin(heading) * SPEED[i];
        BlockPos np = BlockPos.containing(nx, getY(), nz);
        if (sl.hasChunkAt(np)) {
            double ny = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(nx), (int) Math.floor(nz));
            this.setPos(nx, ny, nz);
        } else heading += Math.PI;

        // ---- fling entities ----
        AABB box = new AABB(getX() - R, getY() - 4, getZ() - R, getX() + R, getY() + H, getZ() + R);
        List<Entity> list = sl.getEntities(this, box, e -> !(e instanceof TornadoEntity) && !e.isSpectator()
                && !(e instanceof Player p && p.isCreative()));
        for (Entity e : list) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ(), d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            double s = 1 - d / R;
            Entity veh = e.getVehicle();
            DominatorEntity dom = e instanceof DominatorEntity de ? de : (veh instanceof DominatorEntity dv ? dv : null);
            if (dom != null && f <= dom.resistance()) {
                if (e == dom) e.setDeltaMovement(e.getDeltaMovement().add(-dz / (d + 1) * 0.03, 0, dx / (d + 1) * 0.03));
                continue;
            }
            if (veh != null && dom == null) continue; // carried by something else
            double inv = d < 0.001 ? 0 : 1 / d;
            Vec3 tang = new Vec3(-dz * inv, 0, dx * inv).scale((0.30 + 0.05 * f) * (0.4 + s));
            Vec3 in = d < 2 ? Vec3.ZERO : new Vec3(-dx * inv, 0, -dz * inv).scale((0.10 + 0.02 * f) * s);
            double rel = (e.getY() - getY()) / H;
            double lift = rel > 0.85 ? -0.05 : (0.10 + 0.03 * f) * (0.3 + s);
            Vec3 m = e.getDeltaMovement().scale(0.6).add(tang).add(in).add(0, lift, 0);
            if (m.length() > 2.2) m = m.normalize().scale(2.2);
            e.setDeltaMovement(m);
            e.hurtMarked = true;
            if (e instanceof LivingEntity le && age % 10 == 0 && d < R * 0.75) le.hurt(sl.damageSources().generic(), (float) (DAMAGE[i] * (0.4 + s)));
        }

        // ---- rip blocks ----
        if (sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            for (int n = 0; n < BLOCKS_PER_TICK[i]; n++) {
                double a = this.random.nextDouble() * Math.PI * 2, r = Math.sqrt(this.random.nextDouble()) * R * 0.7;
                int x = (int) Math.floor(getX() + Math.cos(a) * r), z = (int) Math.floor(getZ() + Math.sin(a) * r);
                if (!sl.hasChunkAt(new BlockPos(x, 64, z))) continue;
                int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos pos = new BlockPos(x, top - this.random.nextInt(f >= 3 ? 6 : 2), z);
                BlockState st = sl.getBlockState(pos);
                if (st.isAir() || !st.getFluidState().isEmpty() || st.hasBlockEntity()) continue;
                float h = st.getDestroySpeed(sl, pos);
                if (h < 0 || h > HARDNESS[i]) continue;
                if (this.random.nextFloat() < 0.65f) {
                    FallingBlockEntity fb = FallingBlockEntity.fall(sl, pos, st);
                    double dx = fb.getX() - getX(), dz = fb.getZ() - getZ(), d = Math.max(1, Math.sqrt(dx * dx + dz * dz));
                    fb.setDeltaMovement(-dz / d * 0.6 + (this.random.nextDouble() - 0.5) * 0.3, 0.5 + 0.1 * f, dx / d * 0.6 + (this.random.nextDouble() - 0.5) * 0.3);
                    fb.hurtMarked = true;
                } else sl.removeBlock(pos, false);
            }
        }

        // ---- visuals + sound ----
        int rings = 10 + 3 * f;
        for (int k = 0; k < rings; k++) {
            double fr = (double) k / rings, y = getY() + fr * H * 0.9;
            double r = 1.2 + (R * 0.55) * Math.pow(fr, 1.4);
            double ang = age * (0.45 - 0.2 * fr) + k * 1.7;
            particle(sl, ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + Math.cos(ang) * r, y, getZ() + Math.sin(ang) * r, 1, 0.2, 0.1, 0.2, 0.002);
            if (k % 2 == 0) particle(sl, ParticleTypes.CLOUD, getX() + Math.cos(ang + 3) * r * 0.8, y, getZ() + Math.sin(ang + 3) * r * 0.8, 1, 0.3, 0.2, 0.3, 0.01);
        }
        for (int k = 0; k < 4 + f * 2; k++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * R * 0.6;
            particle(sl, ParticleTypes.LARGE_SMOKE, getX() + Math.cos(a) * r, getY() + 0.5, getZ() + Math.sin(a) * r, 1, 0.3, 0.8, 0.3, 0.05);
        }
        if (age % 15 == 0) sl.playSound(null, getX(), getY() + 5, getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 3f + f, 0.5f);
        if (f >= 3 && this.random.nextInt(200) == 0) sl.playSound(null, getX(), getY() + 20, getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8f, 0.8f);
    }

    private static void particle(ServerLevel sl, ParticleOptions o, double x, double y, double z, int n, double dx, double dy, double dz, double sp) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(x, y, z) < 300 * 300) sl.sendParticles(p, o, true, x, y, z, n, dx, dy, dz, sp);
    }
}
