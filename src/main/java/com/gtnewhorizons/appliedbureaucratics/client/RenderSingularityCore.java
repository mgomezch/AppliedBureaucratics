package com.gtnewhorizons.appliedbureaucratics.client;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;

import com.gtnewhorizons.appliedbureaucratics.facility.singularity.TileSingularityCore;

/// Draws the Singularity: a dark sphere about 5 blocks across in radius, with thin rings on tilted, fixed axes, whose
/// pulse follows load and whose colour follows Alignment (teal, amber, red). This is the plain renderer; it uses no
/// shaders, so it is also what clients without shader support see. Every transform is pushed and popped, and every
/// rotation is about a fixed unit axis, per AGENTS.md section 6.
public class RenderSingularityCore extends TileEntitySpecialRenderer {

    private static final float RADIUS = 5;
    private final Sphere sphere = new Sphere();

    @Override
    public void renderTileEntityAt(final TileEntity tile, final double x, final double y, final double z,
        final float partialTicks) {
        if (!(tile instanceof TileSingularityCore core)) {
            return;
        }
        final double time = (tile.getWorldObj()
            .getTotalWorldTime() + partialTicks) / 20.0;
        final float alignment = core.getAlignment();
        final float[] color = alignment >= 60 ? new float[] { 0.1F, 0.8F, 0.75F }
            : alignment >= 30 ? new float[] { 0.95F, 0.65F, 0.1F } : new float[] { 0.9F, 0.15F, 0.1F };
        final float pulse = (float) (0.5 + 0.5 * Math.sin(time * (1 + 3 * core.getLoad())));

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // The body: nearly black, with a faint tint of its mood.
            GL11.glColor4f(color[0] * 0.08F, color[1] * 0.08F, color[2] * 0.08F, 1F);
            this.sphere.setDrawStyle(org.lwjgl.util.glu.GLU.GLU_FILL);
            this.sphere.draw(RADIUS, 32, 24);

            // A glow shell that breathes with the load.
            GL11.glDepthMask(false);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glColor4f(color[0], color[1], color[2], 0.08F + 0.12F * pulse);
            this.sphere.draw(RADIUS * (1.04F + 0.03F * pulse), 32, 24);

            // Rings on tilted axes, turning at different rates.
            GL11.glLineWidth(2F);
            GL11.glColor4f(color[0], color[1], color[2], 0.6F);
            this.ring(RADIUS * 1.5F, (float) (time * 9), 1F, 0F, 0F, 23F);
            this.ring(RADIUS * 1.8F, (float) (-time * 6), 0F, 0F, 1F, -37F);
            this.ring(RADIUS * 2.1F, (float) (time * 4), 1F, 0F, 0F, 61F);
            GL11.glDepthMask(true);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    /// A circle of `radius` in a plane tilted `tilt` degrees about the unit axis (ax, ay, az), spun `spin` degrees
    /// about the vertical.
    private void ring(final float radius, final float spin, final float ax, final float ay, final float az,
        final float tilt) {
        GL11.glPushMatrix();
        GL11.glRotatef(spin, 0F, 1F, 0F);
        GL11.glRotatef(tilt, ax, ay, az);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 96; i++) {
            final double a = i * 2 * Math.PI / 96;
            GL11.glVertex3d(radius * Math.cos(a), 0, radius * Math.sin(a));
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }
}
