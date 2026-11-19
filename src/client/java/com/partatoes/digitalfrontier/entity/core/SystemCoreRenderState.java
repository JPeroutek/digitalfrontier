package com.partatoes.digitalfrontier.entity.core;

import com.partatoes.digitalfrontier.entity.custom.SystemCorePhase;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class SystemCoreRenderState extends LivingEntityRenderState {
    public SystemCorePhase phase = SystemCorePhase.ABSORBING;
}
