package pers.solid.mishang.uc.mixin;

import net.fabricmc.api.EnvType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.mishang.uc.item.HotbarScrollInteraction;

/**
 * 仅在客户端生效：scrollInHotbar 由客户端鼠标滚轮输入触发，专用服务端不会直接调用。
 * 必须限定为 CLIENT，否则服务端也会转换 PlayerInventory，与 AE2WTLib 对 Inventory 的
 * insertStack 注入产生冲突，导致玩家登录时 insertStackInME → hasTerminal 空指针崩溃
 * （AE2WTLib 11.6.3 的 wirelessTerminals.get(key) 无 null 校验），把玩家踢出服务器。
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
  @Shadow
  public abstract ItemStack getMainHandStack();

  @Shadow
  public int selectedSlot;

  /**
   * 当玩家手持快速建造工具并潜行时，不进行滑动，同时修改快速建造工具的类型。
   */
  @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
  public void lockSelection(double scrollAmount, CallbackInfo ci) {
    final ItemStack mainHandStack = this.getMainHandStack();
    if (mainHandStack.getItem() instanceof HotbarScrollInteraction interaction && interaction.shouldLockScroll(selectedSlot, scrollAmount)) {
      ci.cancel();
    }
  }
}
