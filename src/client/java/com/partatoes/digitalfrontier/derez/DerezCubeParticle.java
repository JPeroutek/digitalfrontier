package com.partatoes.digitalfrontier.derez;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.ARGB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One cube of a derezzing body: a small solid cube in the body's colors, tumbling as it falls, settling on the ground
 * and fading out. A cube of a statue (see {@link DerezStyle#VOXEL}) first holds still in place, turned as its part of
 * the model was, until it breaks off and flies. Drawn as real geometry by {@link DerezCubeGroup}.
 */
public class DerezCubeParticle extends Particle {
    // Vanilla's glint, a light violet
    private static final int GLINT = 0xFF9F6BFF;

    final float size;
    private final int color;
    private final float glintPhase;
    private boolean glinting;
    private final Quaternionf base;
    private final Vector3f spinAxis;
    private final float spinSpeed;
    private final int hold;
    private final double releaseX;
    private final double releaseY;
    private final double releaseZ;
    private float spin;
    private float spinO;

    /** A loose cube, flying from the start. */
    public DerezCubeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, float size, int color) {
        this(level, x, y, z, xd, yd, zd, size, color, new Quaternionf(), 0);
    }

    /**
     * A cube that holds still, turned by {@code base}, for {@code hold} ticks, then flies off with the given velocity.
     */
    public DerezCubeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, float size, int color,
                             Quaternionf base, int hold) {
        super(level, x, y, z);
        this.size = size;
        this.color = color;
        this.glintPhase = random.nextFloat() * (float) Math.PI * 2;
        this.base = base;
        this.hold = hold;
        this.releaseX = xd;
        this.releaseY = yd;
        this.releaseZ = zd;
        this.xd = hold > 0 ? 0 : xd;
        this.yd = hold > 0 ? 0 : yd;
        this.zd = hold > 0 ? 0 : zd;
        this.gravity = 0.6f;
        this.friction = 0.96f;
        this.hasPhysics = true;
        this.lifetime = hold + 20 + random.nextInt(21);
        setSize(size, size);
        this.spinAxis = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize();
        this.spinSpeed = 0.15f + random.nextFloat() * 0.35f;
        // A statue's cubes start square to their part; loose ones at any angle
        this.spin = hold > 0 ? 0 : random.nextFloat() * (float) Math.PI * 2;
        this.spinO = spin;
    }

    @Override
    public void tick() {
        if (age < hold) {
            // Standing as part of the statue
            xo = x;
            yo = y;
            zo = z;
            if (++age == hold) {
                xd = releaseX;
                yd = releaseY;
                zd = releaseZ;
            }
            return;
        }
        super.tick();
        spinO = spin;
        // Tumbles in the air, lies still once it lands
        if (!onGround) {
            spin += spinSpeed;
        }
    }

    @Override
    public ParticleRenderType getGroup() {
        return DerezCubeGroup.TYPE;
    }

    /** Makes it shimmer like enchantment glint, if it came from something enchanted. */
    public DerezCubeParticle glinting(boolean glinting) {
        this.glinting = glinting;
        return this;
    }

    /** Its color at a moment between ticks: an enchanted cube washes in and out of the glint's purple. */
    int color(float partialTick) {
        if (!glinting) {
            return color;
        }
        float wash = 0.25f + 0.25f * (float) Math.sin((age + partialTick) * 0.35f + glintPhase);
        return ARGB.srgbLerp(wash, 0xFF000000 | color, GLINT) & 0xFFFFFF;
    }

    /** Whether it's still standing as part of a statue. */
    boolean standing() {
        return age < hold;
    }

    /** Its rotation at a moment between ticks. */
    Quaternionf rotation(float partialTick) {
        return new Quaternionf().rotateAxis(spinO + (spin - spinO) * partialTick, spinAxis).mul(base);
    }

    /** How opaque it is, fading over the last third of its flight. */
    float alpha(float partialTick) {
        float flight = lifetime - hold;
        float left = (lifetime - age - partialTick) / (flight / 3f);
        return Math.clamp(left, 0, 1);
    }

    double x(float partialTick) {
        return xo + (x - xo) * partialTick;
    }

    double y(float partialTick) {
        return yo + (y - yo) * partialTick;
    }

    double z(float partialTick) {
        return zo + (z - zo) * partialTick;
    }
}
