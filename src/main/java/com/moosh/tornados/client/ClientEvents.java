package com.moosh.tornados.client;

import com.moosh.tornados.TornadoMod;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TornadoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(DominatorModel.LAYER, DominatorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(TornadoMod.TORNADO.get(), NoopRenderer::new);
        for (int i = 0; i < 4; i++) { final int lvl = i + 1; e.registerEntityRenderer(TornadoMod.DOMINATORS[i].get(), ctx -> new DominatorRenderer(ctx, lvl)); }
    }
}
