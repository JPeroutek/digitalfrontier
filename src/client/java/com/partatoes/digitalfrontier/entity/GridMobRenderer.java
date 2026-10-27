package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * The grid's creatures on the Program model: each picks its texture per entity, and corrupted ones jitter. Now
 * and then a frame is drawn a little off where the entity really is, more often and further the more corrupted
 * the creature, like a signal breaking up.
 */
// ProgramModel is a PlayerEntityModel, which fixes the render state type; State extends it to carry the texture
public class GridMobRenderer<T extends MobEntity> extends BipedEntityRenderer<T, PlayerEntityRenderState, ProgramModel<PlayerEntityRenderState>> {
    private final Function<T, Identifier> texture;
    private final float jitter;

    public static class State extends PlayerEntityRenderState {
        Identifier texture;
    }

    /**
     * @param jitter 0 for none; around 0.5 for corrupted programs, 1 for the most corrupted
     */
    public GridMobRenderer(EntityRendererFactory.Context context, Function<T, Identifier> texture, float jitter) {
        super(context, new ProgramModel<>(context.getPart(ModModelLayers.PROGRAM)), 0.5f);
        this.texture = texture;
        this.jitter = jitter;
    }

    /** One of a few textures, chosen per entity so a crowd isn't all identical. */
    public static <T extends MobEntity> Function<T, Identifier> variants(String... names) {
        Identifier[] textures = new Identifier[names.length];
        for (int i = 0; i < names.length; i++) {
            textures[i] = Identifier.of(DigitalFrontier.MOD_ID, "textures/entity/" + names[i] + ".png");
        }
        return entity -> textures[Math.floorMod(entity.getUuid().hashCode(), textures.length)];
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(T entity, PlayerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        ((State) state).texture = texture.apply(entity);
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        return ((State) state).texture;
    }

    @Override
    public void render(PlayerEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean glitch = jitter > 0 && random.nextFloat() < 0.12f * jitter;
        if (glitch) {
            matrices.push();
            float offset = 0.15f * jitter;
            matrices.translate((random.nextFloat() - 0.5f) * offset, (random.nextFloat() - 0.5f) * offset * 0.5f, (random.nextFloat() - 0.5f) * offset);
        }
        super.render(state, matrices, vertexConsumers, light);
        if (glitch) {
            matrices.pop();
        }
    }
}
