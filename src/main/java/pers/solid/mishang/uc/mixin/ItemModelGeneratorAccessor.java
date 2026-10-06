package pers.solid.mishang.uc.mixin;

import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import com.google.gson.JsonElement;

/**
 * 1.18.2 中 {@link ItemModelGenerator#writer} 是 private，而 1.20.1 中可访问。 该访问器用于读取模型输出回调，以便手写自定义模型 JSON。
 *
 * <p>In 1.18.2 {@link ItemModelGenerator#writer} is private, while it is accessible in 1.20.1. This accessor
 * exposes the model output callback so custom model JSON can be written manually.
 */
@Mixin(ItemModelGenerator.class)
public interface ItemModelGeneratorAccessor {
  @Accessor("writer")
  BiConsumer<Identifier, Supplier<JsonElement>> getWriter();
}
