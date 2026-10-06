package pers.solid.mishang.uc.mixin;

import net.minecraft.entity.mob.SlimeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.18.2 中 {@link SlimeEntity#setSize(int, boolean)} 是 protected 的，此处通过 mixin 暴露。
 */
@Mixin(SlimeEntity.class)
public interface SlimeEntityAccessor {
  @Invoker("setSize")
  void invokeSetSize(int size, boolean heal);
}
