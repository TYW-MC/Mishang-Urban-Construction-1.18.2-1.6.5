package pers.solid.mishang.uc.data;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Mishanguc 的标签构建器。
 * <p>
 * 1.18.2 中的 {@link FabricTagProvider.FabricTagBuilder} 是 <b>final</b> 的，无法被继承，
 * 因此这里改为组合（包装）方式：内部持有一个 {@code FabricTagBuilder} 并转发所有调用。
 */
public class MishangucTagBuilder<T> {
  private final TagKey<T> tagKey;
  private final Function<T, RegistryKey<T>> valueToKey;
  private final FabricTagProvider<T>.FabricTagBuilder<T> delegate;

  protected MishangucTagBuilder(TagKey<T> tagKey, Function<T, RegistryKey<T>> valueToKey, FabricTagProvider<T>.FabricTagBuilder<T> delegate) {
    this.tagKey = tagKey;
    this.valueToKey = valueToKey;
    this.delegate = delegate;
  }

  public MishangucTagBuilder<T> add(T value) {
    delegate.add(valueToKey.apply(value));
    return this;
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> add(T... values) {
    Stream.of(values).map(this.valueToKey).forEach(delegate::add);
    return this;
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> add(RegistryKey<T>... keys) {
    delegate.add(keys);
    return this;
  }

  public MishangucTagBuilder<T> add(Identifier id) {
    delegate.add(id);
    return this;
  }

  public MishangucTagBuilder<T> addOptional(Identifier id) {
    delegate.addOptional(id);
    return this;
  }

  public MishangucTagBuilder<T> addOptional(RegistryKey<T> key) {
    delegate.addOptional(key);
    return this;
  }

  public MishangucTagBuilder<T> addTag(TagKey<T> identifiedTag) {
    delegate.addTag(identifiedTag);
    return this;
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> addTag(TagKey<T>... tags) {
    for (TagKey<T> tag : tags) {
      delegate.addTag(tag);
    }
    return this;
  }

  public MishangucTagBuilder<T> addTag(MishangucTagBuilder<T> builder) {
    return addTag(builder.tagKey);
  }

  @SafeVarargs
  public final MishangucTagBuilder<T> addTag(MishangucTagBuilder<T>... builders) {
    Arrays.stream(builders).map(b -> b.tagKey).forEach(this::addTag);
    return this;
  }

  public MishangucTagBuilder<T> addOptionalTag(Identifier id) {
    delegate.addOptionalTag(id);
    return this;
  }

  public MishangucTagBuilder<T> addOptionalTag(TagKey<T> tag) {
    delegate.addOptionalTag(tag);
    return this;
  }

  public MishangucTagBuilder<T> forceAddTag(TagKey<T> tag) {
    delegate.forceAddTag(tag);
    return this;
  }

  public MishangucTagBuilder<T> setReplace(boolean replace) {
    delegate.setReplace(replace);
    return this;
  }
}
