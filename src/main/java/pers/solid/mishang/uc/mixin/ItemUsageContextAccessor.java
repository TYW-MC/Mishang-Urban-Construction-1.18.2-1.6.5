package pers.solid.mishang.uc.mixin;

import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.18.2 中 {@link ItemUsageContext#getHitResult()} 是 protected 的，而 1.20.1 中为 public。 该访问器用于在
 * 1.18.2 中恢复对命中结果的访问能力。
 *
 * <p>In 1.18.2 {@link ItemUsageContext#getHitResult()} is protected, while it is public in 1.20.1. This
 * invoker restores access to the hit result in 1.18.2.
 */
@Mixin(ItemUsageContext.class)
public interface ItemUsageContextAccessor {
  @Invoker("getHitResult")
  BlockHitResult invokeGetHitResult();
}
