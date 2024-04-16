package com.partatoes.digitalfrontier.particle;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;


public class LuminanceDustParticleEffect extends DustParticleEffect {

    public static final Vector3f COLOR = Vec3d.unpackRgb(0xFDB3B5).toVector3f();
    public static final LuminanceDustParticleEffect DEFAULT = new LuminanceDustParticleEffect(COLOR, 1.0f);

    public LuminanceDustParticleEffect(Vector3f vector3f, float f) {
        super(vector3f, f);
    }
}
