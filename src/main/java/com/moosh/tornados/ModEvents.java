package com.moosh.tornados;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModEvents {
    @Mod.EventBusSubscriber(modid = TornadoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void attrs(EntityAttributeCreationEvent e) {
            for (int i = 0; i < 4; i++) e.put(TornadoMod.DOMINATORS[i].get(), DominatorEntity.attributes(i + 1).build());
        }
    }

    @Mod.EventBusSubscriber(modid = TornadoMod.MODID)
    public static class ForgeBus {
        @SubscribeEvent
        public static void commands(RegisterCommandsEvent e) {
            CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mooshgfstornado").requires(s -> s.hasPermission(2));
            for (int f = 1; f <= 5; f++) { final int ff = f; root.then(Commands.literal("f" + f).executes(c -> tornado(c, ff))); }
            for (int n = 1; n <= 4; n++) { final int nn = n; root.then(Commands.literal("dominator" + n).executes(c -> dominator(c, nn))); }
            root.then(Commands.literal("dominator").then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                    .executes(c -> dominator(c, IntegerArgumentType.getInteger(c, "level")))));
            root.then(Commands.literal("remove").executes(ModEvents::remove));
            d.register(root);
        }
    }

    private static int tornado(CommandContext<CommandSourceStack> c, int f) {
        CommandSourceStack s = c.getSource(); ServerLevel lvl = s.getLevel();
        Vec3 look = s.getEntity() != null ? s.getEntity().getLookAngle() : new Vec3(1, 0, 0);
        double x = s.getPosition().x + look.x * (30 + 10 * f), z = s.getPosition().z + look.z * (30 + 10 * f);
        double y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        TornadoEntity t = TornadoMod.TORNADO.get().create(lvl);
        t.setF(f); t.setPos(x, y, z);
        lvl.addFreshEntity(t);
        s.sendSuccess(() -> Component.literal("F" + f + " tornado spawned ahead of you."), true);
        return 1;
    }

    private static int dominator(CommandContext<CommandSourceStack> c, int n) {
        CommandSourceStack s = c.getSource(); ServerLevel lvl = s.getLevel();
        Entity d = ((EntityType<?>) TornadoMod.DOMINATORS[n - 1].get()).create(lvl);
        Vec3 p = s.getPosition();
        d.moveTo(p.x, p.y, p.z, s.getEntity() != null ? s.getEntity().getYRot() : 0, 0);
        lvl.addFreshEntity(d);
        s.sendSuccess(() -> Component.literal("Dominator " + n + " spawned. Right-click to board."), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (Entity e : c.getSource().getLevel().getAllEntities()) if (e instanceof TornadoEntity || e instanceof DominatorEntity) { e.discard(); n++; }
        final int k = n;
        c.getSource().sendSuccess(() -> Component.literal("Removed " + k + " entities."), true);
        return n;
    }
}
