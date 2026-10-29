package com.partatoes.digitalfrontier.particle;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;


public class LuminanceDustParticleEffect extends DustParticleOptions {

    public static final int COLOR = 0xFDB3B5;
    public static final LuminanceDustParticleEffect DEFAULT = new LuminanceDustParticleEffect(COLOR, 1.0f);

    public LuminanceDustParticleEffect(int color, float f) {
        super(color, f);
    }
}
