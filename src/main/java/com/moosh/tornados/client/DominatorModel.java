package com.moosh.tornados.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.moosh.tornados.DominatorEntity;
import com.moosh.tornados.TornadoMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class DominatorModel extends EntityModel<DominatorEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(TornadoMod.MODID, "dominator"), "main");
    private final ModelPart root;

    public DominatorModel(ModelPart root) { this.root = root; }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartPose o = PartPose.offset(0, 24, 0);
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-16, -14, -32, 32, 12, 64), o);
        r.addOrReplaceChild("bumper", CubeListBuilder.create().texOffs(0, 80).addBox(-17, -8, -36, 34, 8, 4), o);
        r.addOrReplaceChild("cabin", CubeListBuilder.create().texOffs(0, 100).addBox(-13, -25, -14, 26, 11, 36), o);
        r.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(130, 100).addBox(-10, -27, -4, 20, 2, 3), o);
        r.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(130, 110).addBox(-3, -30, 8, 6, 5, 6), o);
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override public void setupAnim(DominatorEntity e, float a, float b, float c, float d, float f) { }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
