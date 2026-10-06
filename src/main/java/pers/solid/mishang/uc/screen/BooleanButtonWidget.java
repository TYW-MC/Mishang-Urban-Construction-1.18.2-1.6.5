package pers.solid.mishang.uc.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 用于处理布尔值的按钮。按下鼠标时切换。
 */
@Environment(EnvType.CLIENT)
public class BooleanButtonWidget extends ButtonWidget implements TooltipUpdated {
  public final boolean defaultValue = false;

  /**
   * 通常在没有选中对象时返回 null。
   */
  private final Function<BooleanButtonWidget, @Nullable Boolean> valueGetter;

  private final BooleanConsumer valueSetter;
  public Function<@Nullable Boolean, Text> renderedNameSupplier = null;
  public @Nullable Function<@Nullable Boolean, @Nullable Text> tooltipSupplier = null;
  public @Nullable Text keyboardShortcut = null;
  private Supplier<Text> summaryTextSupplier = null;

  /**
   * 用于布尔值的按钮。
   *
   * @param x           坐标的X值。
   * @param y           坐标的Y值。
   * @param width       按钮的宽度。
   * @param height      按钮的高度。
   * @param message     按钮上显示的文本。是固定的。
   * @param valueGetter 如何获取布尔值？
   * @param valueSetter 如何设置布尔值？
   * @param onPress     按钮按下去的反应。通常为空。
   */
  public BooleanButtonWidget(int x, int y, int width, int height, Text message, Function<BooleanButtonWidget, @Nullable Boolean> valueGetter, BooleanConsumer valueSetter, PressAction onPress) {
    super(x, y, width, height, message, onPress, ButtonWidget.EMPTY);
    this.valueGetter = valueGetter;
    this.valueSetter = valueSetter;
    updateTooltip();
  }

  public BooleanButtonWidget setSummaryTextSupplier(Supplier<Text> summaryTextSupplier) {
    this.summaryTextSupplier = summaryTextSupplier;
    return this;
  }

  public BooleanButtonWidget setRenderedNameSupplier(Function<@Nullable Boolean, Text> renderedNameSupplier) {
    this.renderedNameSupplier = renderedNameSupplier;
    return this;
  }

  public BooleanButtonWidget setRenderedName(Text renderedName) {
    this.renderedNameSupplier = ignore -> renderedName;
    return this;
  }

  public BooleanButtonWidget setTooltipSupplier(Function<@Nullable Boolean, @Nullable Text> tooltipSupplier) {
    this.tooltipSupplier = tooltipSupplier;
    return this;
  }

  public BooleanButtonWidget setTooltip(Text tooltip) {
    this.tooltipSupplier = ignore -> tooltip;
    return this;
  }

  /**
   * 1.18.2 中不能直接设置按钮的 tooltip，因此在此自行存储 tooltip 的内容，并在 {@link #renderTooltip(MatrixStack, int, int)} 中绘制。
   */
  private @Nullable Text tooltipText = null;

  /**
   * 设置按钮的 tooltip 与复述文本。1.18.2 中没有直接设置 tooltip 的 API，因此第一个参数存储到 {@link #tooltipText}，
   * 并在 {@link #renderTooltip(MatrixStack, int, int)} 中绘制；第二个参数（复述文本）在 1.18.2 的 tooltip 机制中无法附加，故忽略。
   */
  public void setTooltip(Text tooltip, Text narration) {
    this.tooltipText = tooltip;
  }

  @Override
  public void renderTooltip(MatrixStack matrices, int mouseX, int mouseY) {
    final Screen screen = MinecraftClient.getInstance().currentScreen;
    if (this.tooltipText != null && screen != null) {
      screen.renderTooltip(matrices, this.tooltipText, mouseX, mouseY);
    }
  }

  public BooleanButtonWidget setKeyboardShortcut(Text text) {
    this.keyboardShortcut = text;
    return this;
  }

  public Text getSummaryMessage() {
    return summaryTextSupplier == null ? super.getMessage() : summaryTextSupplier.get(); // 忽略 renderMessage
  }

  @Override
  public void updateTooltip() {
    final Boolean value = getValue();
    final Text tooltip = tooltipSupplier == null ? null : tooltipSupplier.apply(value);
    final MutableText content = value == null ? TextBridge.empty().append(getSummaryMessage()) : ScreenTexts.composeToggleText(getSummaryMessage(), value);
    final MutableText narration = value == null ? TextBridge.empty() : TextBridge.translatable("narration.mishanguc.button.current_value", value ? ScreenTexts.ON : ScreenTexts.OFF);
    if (tooltip != null) {
      content.append(ScreenTexts.LINE_BREAK).append(tooltip);
      narration.append(ScreenTexts.LINE_BREAK).append(tooltip);
    }
    if (keyboardShortcut != null) {
      MutableText composed = MishangUtils.describeShortcut(keyboardShortcut);
      content.append(ScreenTexts.LINE_BREAK).append(composed);
      narration.append(ScreenTexts.LINE_BREAK).append(composed);
    }
    setTooltip(content, narration);
  }

  public @Nullable Boolean getValue() {
    return valueGetter.apply(this);
  }

  public void setValue(boolean value) {
    valueSetter.accept(value);
    updateTooltip();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (this.active && this.visible && clicked(mouseX, mouseY) && button == 2) {
      this.playDownSound(MinecraftClient.getInstance().getSoundManager());
      setValue(defaultValue);
      return true;
    } else {
      return super.mouseClicked(mouseX, mouseY, button);
    }
  }

  @Override
  protected boolean isValidClickButton(int button) {
    return button == 0 || button == 1;
  }

  @Override
  public void onPress() {
    final Boolean value = getValue();
    if (value != null) {
      setValue(!value);
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    final boolean b = super.mouseScrolled(mouseX, mouseY, amount);
    final Boolean value = getValue();
    if (value != null) {
      setValue(!value);
      return true;
    }
    return b;
  }

  @Override
  public Text getMessage() {
    final Text renderedName = renderedNameSupplier == null ? super.getMessage() : renderedNameSupplier.apply(getValue());
    final @Nullable Boolean value = getValue();
    return value == null
        ? renderedName
        : TextBridge.empty()
        .append(renderedName)
        .styled(style -> style.withColor(value ? 0xb2ff96 : 0xffac96));
  }

  @Override
  protected MutableText getNarrationMessage() {
    // 考虑到部分按钮，比如加粗按钮，显示时只显示“B”，但是事实上复述功能应该复述“加粗”。
    return getNarrationMessage(getSummaryMessage());
  }

  @Override
  protected void appendDefaultNarrations(NarrationMessageBuilder builder) {
    super.appendDefaultNarrations(builder);
    if (getValue() == null) {
      builder.put(NarrationPart.USAGE, TextBridge.translatable("narration.mishanguc.button.null"));
    } else {
      builder.put(NarrationPart.USAGE, TextBridge.translatable("narration.mishanguc.button.boolean_usage"));
    }
  }

  @Override
  public void setFocused(boolean focused) {
    super.setFocused(focused);
    updateTooltip();
  }
}
