package com.partatoes.digitalfrontier.entity.bool;

import com.partatoes.digitalfrontier.entity.custom.BoolSignals;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BoolRenderState extends EntityRenderState {
    public BoolSignals.Signal signal = BoolSignals.Signal.IDLE;
    public boolean sitting;
    /** How far through derezzing it is (0 to 1), or -1 while it's alive. */
    public float derez = -1;
    /** Set on the frame its derez statue should be raised, with the color for its cubes. */
    public int raise = -1;
}
