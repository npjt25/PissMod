package com.npjt25.pissmod.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import com.npjt25.pissmod.PissConfig;

/** Settings screen (default key: K). */
public class PissConfigScreen extends Screen {
	private static final int[] COLORS = {0xF2D53C, 0xFFF4A0, 0xD9A400, 0xD8F4FF, 0x7CFC00, 0xFF4040, 0x4FA0FF, 0xC060FF};
	private static final String[] COLOR_NAMES = {"yellow", "pale", "dark", "clear", "green", "red", "blue", "purple"};

	private final @Nullable Screen parent;

	public PissConfigScreen(@Nullable Screen parent) {
		super(Component.translatable("screen.pissmod.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = this.height / 4 + 10;

		this.addRenderableWidget(new PowerSlider(x, y, 200, 20));
		this.addRenderableWidget(Button.builder(colorLabel(), button -> {
			PissConfig config = PissConfig.get();
			config.setColorRgb(COLORS[(colorIndex() + 1) % COLORS.length]);
			button.setMessage(colorLabel());
		}).bounds(x, y + 54, 200, 20).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(x, y + 100, 200, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int centerX = this.width / 2;
		int y = this.height / 4 + 10;
		centeredText(graphics, this.title, centerX, y - 20);

		PissConfig config = PissConfig.get();
		Component hint = config.streamPower >= config.damageThreshold
				? Component.translatable("screen.pissmod.hint.hurts").withStyle(ChatFormatting.RED)
				: Component.translatable("screen.pissmod.hint.safe", config.damageThreshold).withStyle(ChatFormatting.GRAY);
		centeredText(graphics, hint, centerX, y + 28);
	}

	private void centeredText(GuiGraphicsExtractor graphics, Component text, int centerX, int y) {
		graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		PissConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}

	private static int colorIndex() {
		int rgb = PissConfig.get().colorRgb();

		for (int i = 0; i < COLORS.length; i++) {
			if (COLORS[i] == rgb) {
				return i;
			}
		}

		return -1;
	}

	private static Component colorLabel() {
		int index = colorIndex();
		Component name = index < 0
				? Component.literal(PissConfig.get().streamColor)
				: Component.translatable("screen.pissmod.color." + COLOR_NAMES[index]);
		return Component.translatable("screen.pissmod.color", name.copy().withColor(PissConfig.get().colorRgb()));
	}

	private static final class PowerSlider extends AbstractSliderButton {
		PowerSlider(int x, int y, int width, int height) {
			super(x, y, width, height, CommonComponents.EMPTY, PissConfig.get().streamPower / (double) PissConfig.MAX_POWER);
			this.updateMessage();
		}

		private int power() {
			return (int) Math.round(this.value * PissConfig.MAX_POWER);
		}

		@Override
		protected void updateMessage() {
			int power = this.power();
			String tier = power >= PissConfig.MAX_POWER ? "firehose" : power >= PissConfig.get().damageThreshold ? "painful" : power >= 15 ? "strong" : "normal";
			this.setMessage(Component.translatable("screen.pissmod.power", power, Component.translatable("screen.pissmod.power." + tier)));
		}

		@Override
		protected void applyValue() {
			PissConfig.get().streamPower = this.power();
		}
	}
}
