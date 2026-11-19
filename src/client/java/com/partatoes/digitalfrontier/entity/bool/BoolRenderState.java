package com.partatoes.digitalfrontier.entity.bool;

import com.partatoes.digitalfrontier.entity.custom.BoolSignals;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BoolRenderState extends EntityRenderState {
    public BoolSignals.Signal signal = BoolSignals.Signal.IDLE;
    public boolean sitting;
}
