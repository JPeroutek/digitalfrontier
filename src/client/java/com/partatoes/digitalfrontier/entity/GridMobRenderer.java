package com.partatoes.digitalfrontier.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.DigitalFrontier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * The grid's creatures on the Program model: each picks its texture per entity, and corrupted ones jitter. Now
 * and then a frame is drawn a little off where the entity really is, more often and further the more corrupted
 * the creature, like a signal breaking up.
 */
// State carries the chosen texture; it must not be a player render state, which the player renderer would take over
public class GridMobRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, ProgramModel> {
    private final Function<T, Identifier> texture;
    private final float jitter;

    public static class State extends HumanoidRenderState {
        Identifier texture;
    }

    /**
     * @param jitter 0 for none; around 0.5 for corrupted programs, 1 for the most corrupted
     */
    public GridMobRenderer(EntityRendererProvider.Context context, Function<T, Identifier> texture, float jitter) {
        super(context, new ProgramModel(context.bakeLayer(ModModelLayers.PROGRAM)), 0.5f);
        this.texture = texture;
        this.jitter = jitter;
    }

    /** One of a few textures, chosen per entity so a crowd isn't all identical. */
    public static <T extends Mob> Function<T, Identifier> variants(String... names) {
        Identifier[] textures = new Identifier[names.length];
        for (int i = 0; i < names.length; i++) {
            textures[i] = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/" + names[i] + ".png");
        }
        return entity -> textures[Math.floorMod(entity.getUUID().hashCode(), textures.length)];
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, HumanoidRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        ((State) state).texture = texture.apply(entity);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return ((State) state).texture;
    }

    @Override
    public void submit(HumanoidRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean glitch = jitter > 0 && random.nextFloat() < 0.12f * jitter;
        if (glitch) {
            matrices.pushPose();
            float offset = 0.15f * jitter;
            matrices.translate((random.nextFloat() - 0.5f) * offset, (random.nextFloat() - 0.5f) * offset * 0.5f, (random.nextFloat() - 0.5f) * offset);
        }
        super.submit(state, matrices, queue, cameraState);
        if (glitch) {
            matrices.popPose();
        }
    }
}
