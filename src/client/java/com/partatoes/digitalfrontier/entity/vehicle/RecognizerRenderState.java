package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class RecognizerRenderState extends EntityRenderState {
    public float yaw;
    /** Whether its searchlight is on, and whether it has caught someone. */
    public boolean searching;
    public boolean spotting;
}
