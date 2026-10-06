package pers.solid.mishang.uc.util;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.ScoreText;
import net.minecraft.text.SelectorText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * 实用类，用于在不同版本之间减少代码差异。
 *
 * <p>1.18.2 中 {@code Text} 接口没有 {@code literal}/{@code translatable}/{@code empty} 等静态工厂方法，
 * 这些方法位于各自的实现类构造器（如 {@link LiteralText}、{@link TranslatableText}）。本类将其统一封装。
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ApiStatus.AvailableSince("0.2.4")
@ApiStatus.NonExtendable
public interface TextBridge {
  static MutableText literal(String string) {
    return new LiteralText(string);
  }

  static MutableText translatable(String key) {
    return new TranslatableText(key);
  }

  static MutableText translatable(String key, Object... args) {
    return new TranslatableText(key, args);
  }

  static MutableText empty() {
    return LiteralText.EMPTY.shallowCopy();
  }

  static MutableText keybind(String string) {
    return new KeybindText(string);
  }

  /**
   * 1.20.1 的 {@code Codecs.TEXT} 在 1.18.2 中不存在，此处用 {@link Text.Serializer} + JSON 构造等价 Codec。
   */
  Codec<Text> TEXT_CODEC =
      new Codec<>() {
        @Override
        public <T> DataResult<Pair<Text, T>> decode(DynamicOps<T> ops, T input) {
          // DFU 4.1.27 中 convertTo 直接返回 U（不包装为 DataResult）。
          final JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
          try {
            final Text text = Text.Serializer.fromJson(json);
            return DataResult.success(Pair.of(text, ops.empty()));
          } catch (Exception e) {
            return DataResult.error("Failed to parse text: " + e.getMessage());
          }
        }

        @Override
        public <T> DataResult<T> encode(Text input, DynamicOps<T> ops, T prefix) {
          final JsonElement json = Text.Serializer.toJsonTree(input);
          return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
        }
      };

  /**
   * 翻译键，带后备文本（1.20.1 的 {@code Text.translatableWithFallback} 在 1.18.2 中不存在）。 1.18.2 无法表达「带后备的翻译键」，
   * 因此直接退化为后备文本（在无语言文件时行为一致）。
   */
  static MutableText translatableWithFallback(String key, String fallback) {
    return new TranslatableText(key);
  }

  // 1.18.2 中没有 Text.nbt(...) 与 NbtDataSource，且本项目未使用，故移除。

  static MutableText score(String name, String objective) {
    return new ScoreText(name, objective);
  }

  static MutableText selector(String pattern, Optional<Text> separator) {
    return new SelectorText(pattern, separator);
  }

  static boolean isEmpty(Text text) {
    return text.getString().isEmpty();
  }
}
