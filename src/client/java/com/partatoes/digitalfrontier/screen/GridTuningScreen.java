package com.partatoes.digitalfrontier.screen;

import com.partatoes.digitalfrontier.grid.GridKey;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * The Grid Tuning Laser's screen: edits a {@link GridTuning}, shows the grid id it leads to, and copies or pastes it
 * as a grid code. Tuning sends only the code; the server checks it and creates the grid.
 */
public class GridTuningScreen extends Screen {
    private static final int WIDTH = 288;
    private static final int ROW = 24;
    private static final int SWATCH = 16;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFA0A0A0;

    private final BlockPos laserPos;
    private GridTuning tuning;
    private EditBox codeField;
    // Set while widgets are being updated from code, so their change listeners don't feed back into the tuning
    private boolean updating;
    private int left;
    private int top;

    public GridTuningScreen(BlockPos laserPos, GridTuning tuning) {
        super(Component.translatable("screen.digitalfrontier.grid_tuning"));
        this.laserPos = laserPos;
        this.tuning = tuning;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = Math.max(8, (height - 260) / 2);
        int y = top + 16;

        EditBox seedField = new EditBox(font, left + 60, y, WIDTH - 60 - 66, 20,
                Component.translatable("screen.digitalfrontier.grid_tuning.seed"));
        seedField.setMaxLength(32);
        seedField.setValue(Long.toString(tuning.seed()));
        seedField.setResponder(text -> update(tuning.withSeed(GridTuning.parseSeed(text))));
        addRenderableWidget(seedField);
        addRenderableWidget(Button.builder(Component.translatable("screen.digitalfrontier.grid_tuning.random"),
                        button -> seedField.setValue(Long.toString(RandomSource.create().nextLong())))
                .bounds(left + WIDTH - 62, y, 62, 20).build());
        y += ROW;

        addRenderableWidget(new IntSlider(left, y, WIDTH, 0, 100, tuning.corruptionPercent(),
                value -> Component.translatable("screen.digitalfrontier.grid_tuning.corruption", value,
                        GridLayout.strongholdCount(value / 100f)),
                value -> update(tuning.withCorruptionPercent(value))));
        y += ROW;
        addRenderableWidget(new IntSlider(left, y, WIDTH, GridTuning.MIN_CITY_RADIUS, GridTuning.MAX_CITY_RADIUS, tuning.cityRadius(),
                value -> Component.translatable("screen.digitalfrontier.grid_tuning.city_radius", value, value * 16),
                value -> update(tuning.withCityRadius(value))));
        y += ROW;

        int half = (WIDTH - 4) / 2;
        List<Integer> blockSizes = new ArrayList<>();
        for (int size = GridTuning.MIN_BLOCK_SIZE; size <= GridTuning.MAX_BLOCK_SIZE; size++) {
            blockSizes.add(size);
        }
        int third = (WIDTH - 8) / 3;
        addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(value + "×" + value), tuning.blockSize())
                .withValues(blockSizes)
                .create(left, y, third, 20, Component.translatable("screen.digitalfrontier.grid_tuning.block_size"),
                        (button, value) -> update(tuning.withBlockSize(value))));
        addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(String.valueOf(value)), tuning.waypointCount())
                .withValues(GridTuning.WAYPOINT_COUNTS)
                .create(left + third + 4, y, third, 20, Component.translatable("screen.digitalfrontier.grid_tuning.waypoints"),
                        (button, value) -> update(tuning.withWaypointCount(value))));
        addRenderableWidget(CycleButton.<StreetAxis.StreetStyle>builder(
                        value -> Component.translatable("screen.digitalfrontier.grid_tuning.street_style." + value.getSerializedName()), tuning.streetStyle())
                .withValues(StreetAxis.StreetStyle.values())
                .create(left + 2 * (third + 4), y, third, 20, Component.translatable("screen.digitalfrontier.grid_tuning.street_style"),
                        (button, value) -> update(tuning.withStreetStyle(value))));
        y += ROW;

        addRenderableWidget(CycleButton.<GridTuning.Levels>builder(
                        value -> Component.translatable("screen.digitalfrontier.grid_tuning.levels." + value.asString()), tuning.levels())
                .withValues(GridTuning.Levels.values())
                .create(left, y, WIDTH, 20, Component.translatable("screen.digitalfrontier.grid_tuning.levels"),
                        (button, value) -> update(tuning.withLevels(value))));
        y += ROW + 12;

        DyeColor[] colors = DyeColor.values();
        int gap = (WIDTH - colors.length * SWATCH) / (colors.length - 1);
        for (int i = 0; i < colors.length; i++) {
            addRenderableWidget(new ColorToggle(left + i * (SWATCH + gap), y, colors[i]));
        }
        y += SWATCH + 18;

        codeField = new EditBox(font, left, y, WIDTH - 2 * 52, 20, Component.translatable("screen.digitalfrontier.grid_tuning.code"));
        codeField.setMaxLength(64);
        codeField.setValue(tuning.code());
        codeField.setResponder(text -> {
            if (!updating) {
                // Rebuild on the next frame: rebuilding replaces this very widget while it's handling the key press
                GridTuning.fromCode(text).ifPresent(pasted -> minecraft.execute(() -> setTuning(pasted)));
            }
        });
        addRenderableWidget(codeField);
        addRenderableWidget(Button.builder(Component.translatable("screen.digitalfrontier.grid_tuning.copy"),
                        button -> minecraft.keyboardHandler.setClipboard(tuning.code()))
                .bounds(left + WIDTH - 2 * 52 + 4, y, 48, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.digitalfrontier.grid_tuning.paste"),
                        button -> GridTuning.fromCode(minecraft.keyboardHandler.getClipboard()).ifPresent(this::setTuning))
                .bounds(left + WIDTH - 48, y, 48, 20).build());
        y += ROW + 14;

        addRenderableWidget(Button.builder(Component.translatable("screen.digitalfrontier.grid_tuning.tune"), button -> {
                    ClientPlayNetworking.send(new GridTuningNetworking.TunePayload(laserPos, tuning.code()));
                    onClose();
                })
                .bounds(left, y, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + half + 4, y, half, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width / 2, top, WHITE);
        context.drawString(font, Component.translatable("screen.digitalfrontier.grid_tuning.seed"), left, top + 22, GRAY);
        int colorsLabelY = top + 16 + 5 * ROW;
        context.drawString(font, Component.translatable("screen.digitalfrontier.grid_tuning.districts",
                tuning.districtColors().size(), GridTuning.MAX_DISTRICT_COLORS), left, colorsLabelY, GRAY);
        int codeLabelY = colorsLabelY + 12 + SWATCH + 6;
        context.drawString(font, Component.translatable("screen.digitalfrontier.grid_tuning.code"), left, codeLabelY, GRAY);
        context.drawString(font, Component.translatable("screen.digitalfrontier.grid_tuning.grid_id",
                GridKey.of(tuning.toSettings()).toString()), left, codeLabelY + 12 + ROW, WHITE);
    }

    private void update(GridTuning updated) {
        if (updating || !updated.isValid()) {
            return;
        }
        tuning = updated;
        updating = true;
        codeField.setValue(tuning.code());
        updating = false;
    }

    private void setTuning(GridTuning updated) {
        tuning = updated;
        rebuildWidgets();
    }

    private void toggleColor(DyeColor color) {
        List<DyeColor> colors = new ArrayList<>(tuning.districtColors());
        // New colors go on the end, so turning a color off and on again gives back the same grid
        if (!colors.remove(color)) {
            colors.add(color);
        }
        update(tuning.withDistrictColors(colors));
    }

    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int width, int min, int max, int initial, IntFunction<Component> label, IntConsumer onChange) {
            super(x, y, width, 20, Component.empty(), (double) (initial - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        private int intValue() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(label.apply(intValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(intValue());
        }
    }

    // A swatch of one district color; bright border when the grid uses it
    private class ColorToggle extends Button {
        private final DyeColor color;

        ColorToggle(int x, int y, DyeColor color) {
            super(x, y, SWATCH, SWATCH, Component.translatable("color.minecraft." + color.getSerializedName()),
                    button -> toggleColor(color), DEFAULT_NARRATION);
            this.color = color;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta) {
            boolean selected = tuning.districtColors().contains(color);
            int border = selected ? WHITE : (isHovered() ? GRAY : 0xFF303030);
            context.fill(getX(), getY(), getX() + width, getY() + height, border);
            context.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFF000000 | color.getTextureDiffuseColor());
            if (!selected) {
                context.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xA0000000);
            }
        }
    }
}
