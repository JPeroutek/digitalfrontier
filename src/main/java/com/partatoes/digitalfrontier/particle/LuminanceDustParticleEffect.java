package com.partatoes.digitalfrontier.particle;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;


public class LuminanceDustParticleEffect extends DustParticleEffect {

    public static final int COLOR = 0xFDB3B5;
    public static final LuminanceDustParticleEffect DEFAULT = new LuminanceDustParticleEffect(COLOR, 1.0f);

    public LuminanceDustParticleEffect(int color, float f) {
        super(color, f);
    }
}
