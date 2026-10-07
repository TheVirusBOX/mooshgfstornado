package com.moosh.tornados.client;

import com.moosh.tornados.DominatorEntity;
import com.moosh.tornados.TornadoMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DominatorRenderer extends MobRenderer<DominatorEntity, DominatorModel> {
    private final ResourceLocation tex;

    public DominatorRenderer(EntityRendererProvider.Context ctx, int level) {
        super(ctx, new DominatorModel(ctx.bakeLayer(DominatorModel.LAYER)), 1.4f);
        this.tex = new ResourceLocation(TornadoMod.MODID, "textures/entity/dominator" + level + ".png");
    }

    @Override public ResourceLocation getTextureLocation(DominatorEntity e) { return tex; }
    @Override protected float getFlipDegrees(DominatorEntity e) { return 0f; }
    @Override protected boolean shouldShowName(DominatorEntity e) { return false; }
}
