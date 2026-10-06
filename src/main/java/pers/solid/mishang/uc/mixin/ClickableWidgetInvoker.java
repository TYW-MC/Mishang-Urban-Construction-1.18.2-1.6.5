package pers.solid.mishang.uc.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.18.2 中 {@link ClickableWidget#setFocused(boolean)} 是 protected（1.20.1 起为公开），
 * 而部分逻辑需要在控件外部清除聚焦状态（例如预设按钮隐藏后仍保持聚焦，导致 Tab 键导航错乱）。
 */
@Mixin(ClickableWidget.class)
public interface ClickableWidgetInvoker {
  @Invoker("setFocused")
  void invokeSetFocused(boolean focused);
}
