package com.partatoes.digitalfrontier.screen;

import com.partatoes.digitalfrontier.grid.GridKey;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

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
    private TextFieldWidget codeField;
    // Set while widgets are being updated from code, so their change listeners don't feed back into the tuning
    private boolean updating;
    private int left;
    private int top;

    public GridTuningScreen(BlockPos laserPos, GridTuning tuning) {
        super(Text.translatable("screen.digitalfrontier.grid_tuning"));
        this.laserPos = laserPos;
        this.tuning = tuning;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = Math.max(8, (height - 236) / 2);
        int y = top + 16;

        TextFieldWidget seedField = new TextFieldWidget(textRenderer, left + 60, y, WIDTH - 60 - 66, 20,
                Text.translatable("screen.digitalfrontier.grid_tuning.seed"));
        seedField.setMaxLength(32);
        seedField.setText(Long.toString(tuning.seed()));
        seedField.setChangedListener(text -> update(tuning.withSeed(GridTuning.parseSeed(text))));
        addDrawableChild(seedField);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.digitalfrontier.grid_tuning.random"),
                        button -> seedField.setText(Long.toString(Random.create().nextLong())))
                .dimensions(left + WIDTH - 62, y, 62, 20).build());
        y += ROW;

        addDrawableChild(new IntSlider(left, y, WIDTH, 0, 100, tuning.corruptionPercent(),
                value -> Text.translatable("screen.digitalfrontier.grid_tuning.corruption", value,
                        GridLayout.strongholdCount(value / 100f)),
                value -> update(tuning.withCorruptionPercent(value))));
        y += ROW;
        addDrawableChild(new IntSlider(left, y, WIDTH, GridTuning.MIN_CITY_RADIUS, GridTuning.MAX_CITY_RADIUS, tuning.cityRadius(),
                value -> Text.translatable("screen.digitalfrontier.grid_tuning.city_radius", value, value * 16),
                value -> update(tuning.withCityRadius(value))));
        y += ROW;

        int half = (WIDTH - 4) / 2;
        List<Integer> blockSizes = new ArrayList<>();
        for (int size = GridTuning.MIN_BLOCK_SIZE; size <= GridTuning.MAX_BLOCK_SIZE; size++) {
            blockSizes.add(size);
        }
        addDrawableChild(CyclingButtonWidget.<Integer>builder(value -> Text.literal(value + "×" + value))
                .values(blockSizes)
                .initially(tuning.blockSize())
                .build(left, y, half, 20, Text.translatable("screen.digitalfrontier.grid_tuning.block_size"),
                        (button, value) -> update(tuning.withBlockSize(value))));
        addDrawableChild(CyclingButtonWidget.<Integer>builder(value -> Text.literal(String.valueOf(value)))
                .values(GridTuning.WAYPOINT_COUNTS)
                .initially(tuning.waypointCount())
                .build(left + half + 4, y, half, 20, Text.translatable("screen.digitalfrontier.grid_tuning.waypoints"),
                        (button, value) -> update(tuning.withWaypointCount(value))));
        y += ROW + 12;

        DyeColor[] colors = DyeColor.values();
        int gap = (WIDTH - colors.length * SWATCH) / (colors.length - 1);
        for (int i = 0; i < colors.length; i++) {
            addDrawableChild(new ColorToggle(left + i * (SWATCH + gap), y, colors[i]));
        }
        y += SWATCH + 18;

        codeField = new TextFieldWidget(textRenderer, left, y, WIDTH - 2 * 52, 20, Text.translatable("screen.digitalfrontier.grid_tuning.code"));
        codeField.setMaxLength(64);
        codeField.setText(tuning.code());
        codeField.setChangedListener(text -> {
            if (!updating) {
                // Rebuild on the next frame: rebuilding replaces this very widget while it's handling the key press
                GridTuning.fromCode(text).ifPresent(pasted -> client.execute(() -> setTuning(pasted)));
            }
        });
        addDrawableChild(codeField);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.digitalfrontier.grid_tuning.copy"),
                        button -> client.keyboard.setClipboard(tuning.code()))
                .dimensions(left + WIDTH - 2 * 52 + 4, y, 48, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.digitalfrontier.grid_tuning.paste"),
                        button -> GridTuning.fromCode(client.keyboard.getClipboard()).ifPresent(this::setTuning))
                .dimensions(left + WIDTH - 48, y, 48, 20).build());
        y += ROW + 14;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.digitalfrontier.grid_tuning.tune"), button -> {
                    ClientPlayNetworking.send(new GridTuningNetworking.TunePayload(laserPos, tuning.code()));
                    close();
                })
                .dimensions(left, y, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(left + half + 4, y, half, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top, WHITE);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.digitalfrontier.grid_tuning.seed"), left, top + 22, GRAY);
        int colorsLabelY = top + 16 + 4 * ROW;
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.digitalfrontier.grid_tuning.districts",
                tuning.districtColors().size(), GridTuning.MAX_DISTRICT_COLORS), left, colorsLabelY, GRAY);
        int codeLabelY = colorsLabelY + 12 + SWATCH + 6;
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.digitalfrontier.grid_tuning.code"), left, codeLabelY, GRAY);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.digitalfrontier.grid_tuning.grid_id",
                GridKey.of(tuning.toSettings()).toString()), left, codeLabelY + 12 + ROW, WHITE);
    }

    private void update(GridTuning updated) {
        if (updating || !updated.isValid()) {
            return;
        }
        tuning = updated;
        updating = true;
        codeField.setText(tuning.code());
        updating = false;
    }

    private void setTuning(GridTuning updated) {
        tuning = updated;
        clearAndInit();
    }

    private void toggleColor(DyeColor color) {
        List<DyeColor> colors = new ArrayList<>(tuning.districtColors());
        // New colors go on the end, so turning a color off and on again gives back the same grid
        if (!colors.remove(color)) {
            colors.add(color);
        }
        update(tuning.withDistrictColors(colors));
    }

    private static class IntSlider extends SliderWidget {
        private final int min;
        private final int max;
        private final IntFunction<Text> label;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int width, int min, int max, int initial, IntFunction<Text> label, IntConsumer onChange) {
            super(x, y, width, 20, Text.empty(), (double) (initial - min) / (max - min));
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
    private class ColorToggle extends ButtonWidget {
        private final DyeColor color;

        ColorToggle(int x, int y, DyeColor color) {
            super(x, y, SWATCH, SWATCH, Text.translatable("color.minecraft." + color.asString()),
                    button -> toggleColor(color), DEFAULT_NARRATION_SUPPLIER);
            this.color = color;
            setTooltip(Tooltip.of(getMessage()));
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean selected = tuning.districtColors().contains(color);
            int border = selected ? WHITE : (isHovered() ? GRAY : 0xFF303030);
            context.fill(getX(), getY(), getX() + width, getY() + height, border);
            context.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFF000000 | color.getEntityColor());
            if (!selected) {
                context.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xA0000000);
            }
        }
    }
}
