package pers.solid.mishang.uc.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.world.dimension.AreaHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.mishang.uc.blocks.ColoredBlocks;

@Mixin(AreaHelper.class)
public abstract class AreaHelperMixin {
  /**
   * 1.18.2 中 {@code AreaHelper.validStateInsidePortal} 仍然存在，用于判断方块是否是传送门内部的有效方块。
   */
  @ModifyReturnValue(method = "validStateInsidePortal", at = @At("RETURN"))
  private static boolean validColoredPortal(boolean original, @Local(argsOnly = true, ordinal = 0) BlockState state) {
    return original || state.isOf(ColoredBlocks.COLORED_NETHER_PORTAL);
  }

  /**
   * 1.18.2 的 {@code AreaHelper} 中没有 {@code getPotentialHeight}，
   * 框架方块判定使用的是 {@code IS_VALID_FRAME_BLOCK} 对应的 {@code method_30487}
   * （即 {@code state.isPortalFrame(world, pos)}）。这里改成让彩色传送门方块也能作为框架。
   */
  @ModifyReturnValue(method = "method_30487", at = @At("RETURN"))
  private static boolean redirectedPotentialHeight(boolean original, @Local(argsOnly = true, ordinal = 0) BlockState state) {
    return original || state.isOf(ColoredBlocks.COLORED_NETHER_PORTAL);
  }
}
