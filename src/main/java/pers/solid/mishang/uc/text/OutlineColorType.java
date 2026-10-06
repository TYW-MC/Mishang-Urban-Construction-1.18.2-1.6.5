package pers.solid.mishang.uc.text;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * 控制告示牌内文本的描边渲染类型。此数值原先是使用的和描边颜色共用整数值表示，使用 -2 表示没有描边，-1 表示自动描边，但这样出现了问题，会与 #fffffeff 和 #ffffffff（这里将 alpha 写最后）。因此加入了专门的枚举与原来的表示方式作区分。
 */
public enum OutlineColorType implements StringIdentifiable {
  NONE("none"),
  AUTO("auto"),
  CUSTOM("custom");

  /**
   * 1.18.2 的 {@link StringIdentifiable} 没有内嵌的 {@code Codec} 类型，其 {@code createCodec} 返回的是普通的
   * {@link Codec}，也没有 {@code byId} 方法，因此这里额外提供了 {@link #byId(String)} 作为替代。
   */
  public static final Codec<OutlineColorType> CODEC = StringIdentifiable.createCodec(OutlineColorType::values, OutlineColorType::valueOf);

  private final String name;

  OutlineColorType(String name) {
    this.name = name;
  }

  /**
   * 根据名称查找对应的枚举值，找不到时返回 {@code null}（与 1.20 的 {@code StringIdentifiable.Codec#byId} 行为一致）。
   */
  public static OutlineColorType byId(String name) {
    for (OutlineColorType value : values()) {
      if (value.name.equals(name)) {
        return value;
      }
    }
    return null;
  }

  @Override
  public String asString() {
    return name;
  }

  public static OutlineColorType fromCompatibilityValue(int outlineColor) {
    return switch (outlineColor) {
      case -2 -> NONE;
      case -1 -> AUTO;
      default -> CUSTOM;
    };
  }

  public int toCompatibilityValue(int outlineColor) {
    return switch (this) {
      case NONE -> -2;
      case AUTO -> -1;
      case CUSTOM -> outlineColor;
    };
  }
}
