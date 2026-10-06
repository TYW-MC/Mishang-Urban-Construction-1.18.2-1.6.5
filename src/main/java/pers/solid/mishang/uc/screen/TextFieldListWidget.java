package pers.solid.mishang.uc.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Narratable;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.*;
import org.lwjgl.glfw.GLFW;
import pers.solid.mishang.uc.Mishanguc;
import pers.solid.mishang.uc.mixin.ContainerWidgetAccessor;
import pers.solid.mishang.uc.text.SpecialDrawable;
import pers.solid.mishang.uc.text.TextContext;
import pers.solid.mishang.uc.util.TextBridge;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本框列表的屏幕。每个列表项都是一个文本框（实际上就是把 {@link TextFieldWidget} 包装成了 {@link Entry}。<p>
 * 此类原本是 {@link AbstractSignBlockEditScreen} 的内部类，后面独立出来了。
 */
@Environment(EnvType.CLIENT)
public class TextFieldListWidget extends AlwaysSelectedEntryListWidget<TextFieldListWidget.Entry> {
  /**
   * 被选中的多个项的列表，通常包含 {@link #selected} 的对象但不一定。一般通过 {@link Entry#setSelected(boolean)} 来修改。
   */
  protected final @NotNull Set<@NotNull Entry> selectedEntries = new HashSet<>();
  private final AbstractSignBlockEditScreen<?> signBlockEditScreen;
  private boolean simplified;
  /**
   * 用于显示时渲染背景时的高度。通常情况下与 {@link #height} 保持一致，但简化模式下会使用不一致的值。
   */
  protected int heightForBackground;
  /**
   * 用于在简化模式下渲染内容的高度。在简化模式下，此值与实际的 {@link #height} 保持一致。
   */
  protected int cuttingHeight = 48;
  /**
   * 在按住 Shift 进行多选时，多选起始的元素。在非 Shift 模式下进行任意选择后，此字段清空。
   */
  private @Nullable Entry startContEntry;

  public TextFieldListWidget(AbstractSignBlockEditScreen<?> signBlockEditScreen,
                             MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
    super(client, width, height, top, bottom, itemHeight);
    this.signBlockEditScreen = signBlockEditScreen;
    this.setRenderBackground(false);
    this.setRenderHeader(false, 0);
    this.setRenderSelection(false);
    this.heightForBackground = bottom - top;
  }

  /**
   * 类似于 {@link #setFocused(Element)}，但是支持在调用 {@link #setSelected(Entry, boolean, boolean)} 时指定参数。
   */
  public void setFocusedAndSelected(@Nullable Entry focused, boolean multiSel, boolean contSel) {
    Entry entry = this.getFocused();
    if (entry != focused && entry instanceof ParentElement parentElement) {
      parentElement.setFocused(null);
    }

    ((ContainerWidgetAccessor) this).setFocusedRaw(focused);
    this.setSelected(focused, multiSel, contSel);
    if (focused != null) {
      this.ensureVisible(focused);
    }
  }

  /**
   * 1.18.2 中 {@link EntryListWidget#mouseClicked} 只会调用 {@link #setFocused(Element)}，不会调用
   * {@link #setSelected(Entry)}（1.20.1 的 {@link Element} 有 {@code setFocused(boolean)} 会向下传播，
   * 1.18.2 完全移除了这套机制）。因此这里补上选中状态的同步，否则会出现以下问题：
   * <ul>
   *   <li>点击某一行后，文字输入进入被点击的行，但属性面板（大小、颜色等）仍作用于之前选中的行；</li>
   *   <li>旧行的文本框焦点没有被清除，导致多行同时显示白色高亮；</li>
   *   <li>重新打开界面时没有选中任何行，属性按钮全部失效，只有文字能改。</li>
   * </ul>
   */
  @Override
  public void setFocused(@Nullable Element focused) {
    super.setFocused(focused);
    if (focused instanceof Entry entry) {
      setSelected(entry, Screen.hasControlDown(), Screen.hasShiftDown());
    }
  }

  /**
   * 设置当前 TextFieldListScreen 的已选中的文本框。
   *
   * @param entry 需要选中的 {@link Entry}。
   * @implNote 此对象的 {@link #selected} 一般不是 null，而 {@link #focused} 会在此对象（{@link TextFieldListWidget}）失焦时变成 {@code null}。
   * @see AbstractSignBlockEditScreen#setFocused(Element)
   */
  @Override
  public void setSelected(@Nullable TextFieldListWidget.Entry entry) {
    setSelected(entry, Screen.hasControlDown(), Screen.hasShiftDown());
  }

  /**
   * 设置当前 TextFieldListScreen 的已选中的文本框。
   *
   * @param entry    需要选中的 {@link Entry}。
   * @param multiSel 是否多选。如果为 {@code false}，则之前已经选中的其他元素将会未选中。
   * @param contSel  是否连续选。如果为 {@code true}，则将之前选中的和当前选中的均选中。
   * @see AbstractSignBlockEditScreen#setFocused(Element)
   */
  public void setSelected(@Nullable TextFieldListWidget.Entry entry, boolean multiSel, boolean contSel) {
    final Entry prevSelected = getSelectedOrNull();
    super.setSelected(entry);

    // 1.18.2 中没有 getNavigationType，因此不再判断焦点是否来自键盘导航。
    if (entry instanceof Entry) {
      if (contSel) {
        if (startContEntry == null) {
          startContEntry = prevSelected;
        }
      } else {
        startContEntry = null;
      }
      final int contFrom = contSel ? children().indexOf(startContEntry) : -1;
      if (!multiSel) {
        for (Entry selectedEntry : Set.copyOf(selectedEntries)) {
          selectedEntry.setSelected(false);
        }
      }

      final int contUntil = contSel ? children().indexOf(entry) : -1;
      if (contFrom != -1 && contUntil != -1 && contFrom != contUntil) {
        final int min = Math.min(contFrom, contUntil);
        final int max = Math.max(contFrom, contUntil);

        for (int i = min; i <= max; i++) {
          final Entry entry1 = children().get(i);
          entry1.setSelected(true);
        }
      } else if (multiSel && selectedEntries.contains(entry)) {
        // 在多选模式下，如果再次选中同一个，则失掉这个选择。
        entry.setSelected(false);
        if (getSelectedOrNull() == entry) {
          super.setSelected(null);
        }
      } else {
        entry.setSelected(true);
      }
    }

    // 更新屏幕按钮中的一些 tooltip
    for (Element child : signBlockEditScreen.children()) {
      if (child instanceof TooltipUpdated tooltipUpdated) {
        tooltipUpdated.updateTooltip();
      }
    }
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (!children().isEmpty()) {
      if (keyCode == GLFW.GLFW_KEY_UP) {
        setFocused(children().get(MathHelper.floorMod(children().indexOf(getSelectedOrNull()) - 1, children().size())));
        return true;
      } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
        setFocused(children().get(MathHelper.floorMod(children().indexOf(getSelectedOrNull()) + 1, children().size())));
        return true;
      }
    } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
      // 此时，children().isEmpty() 为 true
      final Entry newEntry = addEmptyTextField(0);
      TextFieldListWidget.this.setFocusedAndSelected(newEntry, false, false);
      signBlockEditScreen.setFocused(TextFieldListWidget.this);
      return true;
    }
    if (selectedEntries.size() > 1) {
      boolean success = false;
      for (Entry selectedEntry : List.copyOf(selectedEntries)) {
        success = selectedEntry.keyPressed(keyCode, scanCode, modifiers) || success;
      }
      return success;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    if (selectedEntries.size() > 1) {
      boolean success = false;
      for (Entry selectedEntry : selectedEntries) {
        success = selectedEntry.charTyped(chr, modifiers) || success;
      }
      return success;
    }
    return super.charTyped(chr, modifiers);
  }

  @Override
  public int getRowWidth() {
    return width;
  }

  /**
   * 在设置高度的同时，会同时更新自身的高度。注意即使是在 simplified 模式下，参数 {@code height} 的值仍应是完整的高度，如 {@link #heightForBackground}，而非 {@link #cuttingHeight} 的值，通常也不应该传入 {@link #height}。
   */
  @Override
  public void updateSize(int width, int height, int top, int bottom) {
    super.updateSize(width, height, top, bottom);
    this.heightForBackground = bottom - top;
    setWidth(width);
  }

  public void setWidth(int width) {
    this.width = width;
    final boolean scrollbarVisible = getMaxScroll() > 0;
    final int elementWidth = width - (scrollbarVisible ? 10 : 4);
    for (Entry child : children()) {
      child.textFieldWidget.setWidth(elementWidth);
    }
  }

  @Override
  protected int getScrollbarPositionX() {
    return width - 6;
  }

  @ApiStatus.AvailableSince("mc1.17")
  @Override
  public void appendNarrations(NarrationMessageBuilder builder) {
    builder.put(NarrationPart.TITLE, TextBridge.translatable("narration.mishanguc.text_field_list"));
    builder.put(NarrationPart.USAGE, TextBridge.translatable("narration.mishanguc.text_field_list.usage"));
    super.appendNarrations(builder);
  }

  /**
   * 1.18.2 的 {@link EntryListWidget} 没有单独的选中高亮绘制方法，因此改为覆写 {@link #renderList}，
   * 在绘制列表条目之前，先为多选的条目绘制白色高亮。
   */
  @Override
  protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
    for (Entry entry : selectedEntries) {
      final int index = children().indexOf(entry);
      if (index >= 0) {
        final int rowTop = getRowTop(index);
        DrawableHelper.fill(matrices, 1, rowTop - 1, width - 1, rowTop + itemHeight + 4, 0xe0ffffff);
      }
    }
    super.renderList(matrices, x, y, mouseX, mouseY, delta);
  }

  @Contract(pure = true)
  protected boolean isSimplified() {
    return simplified;
  }

  protected void setSimplified(boolean simplified) {
    this.simplified = simplified;
    if (simplified) {
      this.setRenderHorizontalShadows(false);
      this.bottom = top + cuttingHeight;
    } else {
      this.setRenderHorizontalShadows(true);
      this.bottom = top + heightForBackground;
    }
    this.setScrollAmount(getScrollAmount());
    final Entry selectedOrNull = getSelectedOrNull();
    if (selectedOrNull != null) {
      ensureVisible(selectedOrNull);
    }
  }

  protected void increaseHeight(int amount) {
    cuttingHeight = (MathHelper.clamp(cuttingHeight + amount, 0, heightForBackground));
    if (simplified) {
      this.bottom = this.top + cuttingHeight;
    }
    setScrollAmount(getScrollAmount()); // 更新滚动以避免滚动溢出
    final Entry selectedOrNull = getSelectedOrNull();
    if (selectedOrNull != null) {
      ensureVisible(selectedOrNull);
    }
  }

  @Override
  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    super.render(matrices, mouseX, mouseY, delta);
    if (simplified) {
      // 在简化模式下，取消了 renderHorizontalShadows，因此在这里补充并重新写。
      // 1.18.2 中 DrawContext 的相关绘制方法改用 RenderSystem 与 DrawableHelper 的静态方法。
      final int bottomForBackground = this.top + heightForBackground;
      RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
      RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
      DrawableHelper.drawTexture(matrices, this.left, 0, 0.0F, 0.0F, this.width, this.top, 32, 32);
      DrawableHelper.drawTexture(matrices, this.left, bottomForBackground, 0.0F, bottomForBackground, this.width, this.height - bottomForBackground, 32, 32);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      // 1.18.2 中没有 RenderLayer.getGuiOverlay，fillGradient 使用继承自 DrawableHelper 的实例方法。
      this.fillGradient(matrices, this.left, this.top, this.right, this.top + 4, -16777216, 0);
      this.fillGradient(matrices, this.left, bottomForBackground - 4, this.right, bottomForBackground, 0, -16777216);
      this.fillGradient(matrices, this.left, this.bottom - 4, this.right, this.bottom, 0, -16777216);
    }
  }

  /**
   * 添加一个文本框。此方法执行时，不会设置任何的选择或聚焦。
   *
   * @param index       添加的文本的位置，可以设置为 -1，表示添加到最后一个。
   * @param textContext 需要添加的 {@link TextContext}。
   * @param isExisting  是否为现有的，如果是，则不会将 {@link AbstractSignBlockEditScreen#changed} 设为 <code>true</code>。
   */
  public Entry addTextField(int index, @NotNull TextContext textContext, boolean isExisting) {
    if (!isExisting) {
      signBlockEditScreen.changed = true;
    }
    final Entry newEntry = createEntry(textContext);
    final int newIndex = addEntry(newEntry);
    if (index != -1) {
      final List<Entry> rawChildren = children();
      rawChildren.remove(newIndex);
      rawChildren.add(index, newEntry);
    }
    setScrollAmount(getScrollAmount()); // 此处会调用私有方法 recalculateAllChildrenPositions
    setWidth(width); // 重新设置其宽度

    signBlockEditScreen.updateContentVisibility();

    return newEntry;
  }

  private @NotNull Entry createEntry(@NotNull TextContext textContext) {
    final TextFieldWidget textFieldWidget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 2, 0, signBlockEditScreen.width - 4, 15, TextBridge.empty());
    textFieldWidget.setMaxLength(Integer.MAX_VALUE);
    if (textContext.extra != null) {
      textFieldWidget.setText(String.format("-%s %s", textContext.extra.getId(), textContext.extra.asStringArgs()));
    } else if (textContext.text != null) {
      if (textContext.text instanceof LiteralText literalTextContent && textContext.text.getSiblings().isEmpty() && textContext.text.getStyle().isEmpty()) {
        final String text = literalTextContent.getString();
        if (Pattern.compile("^-(\\w+?) (.+)$").matcher(text).matches()) {
          textFieldWidget.setText("-literal " + text);
        } else {
          textFieldWidget.setText(text);
        }
      } else {
        textFieldWidget.setText("-json " + Text.Serializer.toJson(textContext.text));
      }
    }
    final Entry newEntry = new Entry(textFieldWidget, textContext);
    textFieldWidget.setChangedListener(s -> {
      final TextContext textContext1 = newEntry.textContext;
      final Matcher matcher = Pattern.compile("^-(\\w+?) (.+)$").matcher(s);
      textFieldWidget.setSuggestion(null);
      textFieldWidget.setEditableColor(0xffe0e0e0);
      if (matcher.matches()) {
        final String name = matcher.group(1);
        final String value = matcher.group(2);
        switch (name) {
          case "literal" -> textContext1.text = TextBridge.literal(value);
          case "json" -> {
            try {
              textContext1.text = Text.Serializer.fromLenientJson(value);
            } catch (JsonParseException | IllegalStateException e) {
              textFieldWidget.setEditableColor(0xffff5555);
              // 1.18.2 的 TextFieldWidget 没有 setTooltip，改用 setSuggestion 显示错误消息。
              textFieldWidget.setSuggestion(e.getMessage());
            }
          }
          default -> {
            final SpecialDrawable specialDrawable;
            try {
              specialDrawable = SpecialDrawable.fromStringArgs(textContext1, name, value);
              if (specialDrawable == SpecialDrawable.INVALID) { // 如果为 INVALID 则文本为红色。
                textFieldWidget.setEditableColor(0xffff5555);
              } else if (specialDrawable != null) {
                textContext1.extra = specialDrawable;
                textContext1.text = TextBridge.empty();
              } else {
                textContext1.extra = null;
                textContext1.text = TextBridge.literal(s);
              }
            } catch (CommandSyntaxException e) {
              textFieldWidget.setEditableColor(0xffff5555);
              // 1.18.2 的 TextFieldWidget 没有 setTooltip，改用 setSuggestion 显示错误消息。
              textFieldWidget.setSuggestion(e.getRawMessage().getString());
            }
          }
        }
      } else {
        textContext1.extra = null;
        textContext1.text = TextBridge.literal(s);
      }
      signBlockEditScreen.changed = true;
    });
    return newEntry;
  }

  /**
   * 添加一个新的文本框。
   *
   * @param index 添加到的位置，对应在数组或列表中的次序。
   */

  public Entry addEmptyTextField(int index) {
    // 添加时，默认相当于上一行的。
    final TextContext emptyTextContext = index > 0 ? children().get(index - 1).textContext.clone() : signBlockEditScreen.entity.createDefaultTextContext();
    emptyTextContext.text = null;
    emptyTextContext.extra = null;
    return addTextField(index, emptyTextContext, false);
  }

  /**
   * 移除一行文本。此方法执行时，不会自动选中相邻文本。
   */
  public void removeTextField(int index) {
    final List<Entry> children = children();
    final Entry removedEntry = children.get(index);
    removeEntry(removedEntry);
    removedEntry.setSelected(false);
    // 删除一行元素后，对滚动数量进行一次 clamp，以避免出现过度滚动的情况。
    setScrollAmount(getScrollAmount()); // 此处会调用私有方法 recalculateAllChildrenPositions
    setWidth(width); // 重新设置其宽度

    signBlockEditScreen.updateContentVisibility();
    signBlockEditScreen.changed = true;
  }

  /**
   * 清除所有文本。
   */
  public void clearTextFields() {
    clearEntries();
    for (Entry selectedEntry : selectedEntries) {
      selectedEntry.textFieldWidget.setTextFieldFocused(false);
    }
    selectedEntries.clear();

    signBlockEditScreen.updateContentVisibility();
    signBlockEditScreen.changed = true;
  }

  public void moveUpEntries(Collection<Entry> entries) {
    if (entries.isEmpty()) {
      return;
    }

    // 确保按顺序排序
    final List<Entry> children = children();
    final List<Entry> orderedCopy = children.stream().filter(entries::contains).toList();

    for (Entry entry : orderedCopy) {
      final int i = children.indexOf(entry);
      if (i < 0) {
        Mishanguc.MISHANG_LOGGER.warn("Unexpected entry which is not in children when moving up: {}", entry);
        continue;
      } else if (i == 0) {
        // 顶到了第一元素，不能再移动。
        break;
      }
      final Entry entryAtI = children.get(i);
      children.set(i, children.get(i - 1));
      children.set(i - 1, entryAtI);
    }
  }

  public void moveDownEntries(Collection<Entry> entries) {
    if (entries.isEmpty()) {
      return;
    }

    // 确保按倒序排序
    final List<Entry> children = children();
    final List<Entry> reversedEntries = Lists.reverse(children).stream().filter(entries::contains).toList();

    for (Entry entry : reversedEntries) {
      final int i = children.indexOf(entry);
      if (i < 0) {
        Mishanguc.MISHANG_LOGGER.warn("Unexpected entry which is not in children when moving down: {}", entry);
        continue;
      } else if (i == children.size() - 1) {
        // 顶到了最后元素，不能再移动。
        break;
      }
      final Entry entryAtI = children.get(i);
      children.set(i, children.get(i + 1));
      children.set(i + 1, entryAtI);
    }
  }


  public @UnmodifiableView List<TextContext> getTextContexts() {
    return Lists.transform(children(), input -> input.textContext);
  }

  /**
   * {@link TextFieldListWidget} 中的项。由于 {@link TextFieldWidget} 不是 {@link EntryListWidget.Entry}
   * 的子类，所以对该类进行了包装。
   */
  @Environment(EnvType.CLIENT)
  public class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> implements Narratable {
    public final @NotNull TextFieldWidget textFieldWidget;
    public final @NotNull TextContext textContext;

    public Entry(@NotNull TextFieldWidget textFieldWidget, @NotNull TextContext textContext) {
      this.textFieldWidget = textFieldWidget;
      this.textContext = textContext;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof Entry entry))
        return false;

      return textFieldWidget.equals(entry.textFieldWidget);
    }

    @Override
    public int hashCode() {
      return textFieldWidget.hashCode();
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
      // 1.18.2 的 Entry 本身没有 isFocused，改用其内部 textFieldWidget 的状态（在 setSelected 中同步）。
      if (textFieldWidget.isFocused() && textFieldWidget.isVisible()) {
        DrawableHelper.fill(matrices, textFieldWidget.x - 2, y - 2, textFieldWidget.x + textFieldWidget.getWidth() + 2, y + textFieldWidget.getHeight() + 2, 0xfff0f0f0);
      }
      textFieldWidget.y = y;
      textFieldWidget.render(matrices, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return textFieldWidget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return textFieldWidget.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      switch (keyCode) {
        case GLFW.GLFW_KEY_ENTER -> {
          final List<Entry> children = TextFieldListWidget.this.children();
          final int index = children.indexOf(this);
          if (index + 1 < children.size()) {
            TextFieldListWidget.this.setFocused(children.get(index + 1));
          } else if (children.size() > 0) {
            final Entry entry = addEmptyTextField(index + 1);
            TextFieldListWidget.this.setFocusedAndSelected(entry, false, false);
          }
        }
        case GLFW.GLFW_KEY_BACKSPACE -> {
          if (textFieldWidget.getText().isEmpty()) {
            final int index = TextFieldListWidget.this.children().indexOf(this);
            if (index >= 0) {
              TextFieldListWidget.this.removeTextField(index);
              if (!children().isEmpty()) {
                final Entry nearbyEntry = TextFieldListWidget.this.children().get(MathHelper.clamp(index - 1, 0, children().size() - 1));
                TextFieldListWidget.this.setFocusedAndSelected(nearbyEntry, false, false);
              }
            }
          }
        }
      }
      return super.keyPressed(keyCode, scanCode, modifiers)
          || textFieldWidget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
      return super.isMouseOver(mouseX, mouseY) || textFieldWidget.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
      super.mouseMoved(mouseX, mouseY);
      textFieldWidget.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(
        double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (button == 0 && mouseX >= getScrollbarPositionX() && mouseX < getScrollbarPositionX() + 6) {
        return false;
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
          || textFieldWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      return super.mouseScrolled(mouseX, mouseY, amount)
          || textFieldWidget.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      return super.keyReleased(keyCode, scanCode, modifiers)
          || textFieldWidget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
      return super.charTyped(chr, modifiers) || textFieldWidget.charTyped(chr, modifiers);
    }

    @Override
    public Text getNarration() {
      return textFieldWidget.getMessage();
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
      textFieldWidget.appendNarrations(builder);
    }

    /**
     * <p>标记此元素是否被选中，与 {@link #focused} 有区别，即使 {@link TextFieldListWidget} 对象失焦时，此字段可能仍为 {@code true}，从而确保文本框的边缘能够正常用白色显示。
     * <p>修改此对象时，也会一并修改 {@link #selectedEntries}。
     */
    public void setSelected(boolean selected) {
      // 1.18.2 的 Entry 本身没有 focused 状态，选中状态由其内部 textFieldWidget 承载。
      // （即使是因为焦点转移到其他元素导致此元素未被聚焦，其文本框仍保持聚焦。）
      if (selected) {
        selectedEntries.add(this);
      } else {
        selectedEntries.remove(this);
        // 取消选中时，如果控件内部的 focused 仍指向此行（例如该行已被移除），一并清除，
        // 否则键盘事件会继续被转发到一个已经不在列表中的文本框。
        if (TextFieldListWidget.this.getFocused() == this) {
          ((ContainerWidgetAccessor) TextFieldListWidget.this).setFocusedRaw(null);
        }
      }
      // 1.18.2 中 ClickableWidget.setFocused 是 protected，因此使用公开的 setTextFieldFocused。
      textFieldWidget.setTextFieldFocused(selected);
    }
  }
}
