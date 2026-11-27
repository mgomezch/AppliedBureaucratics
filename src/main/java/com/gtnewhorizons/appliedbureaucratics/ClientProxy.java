package com.gtnewhorizons.appliedbureaucratics;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.appliedbureaucratics.workforce.EntityWorkstationMount;

import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers() {
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
            com.gtnewhorizons.appliedbureaucratics.facility.singularity.TileSingularityCore.class,
            new com.gtnewhorizons.appliedbureaucratics.client.RenderSingularityCore());
        RenderingRegistry.registerEntityRenderingHandler(EntityWorkstationMount.class, new Render() {

            @Override
            public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {}

            @Override
            protected ResourceLocation getEntityTexture(Entity entity) {
                return null;
            }
        });
    }
}
