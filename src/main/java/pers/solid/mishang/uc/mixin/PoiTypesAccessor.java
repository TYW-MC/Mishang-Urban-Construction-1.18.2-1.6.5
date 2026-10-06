package pers.solid.mishang.uc.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(PointOfInterestType.class)
public interface PoiTypesAccessor {
  /**
   * 1.18.2 中 {@code PointOfInterestType.register} 的签名为
   * {@code (String id, Set<BlockState> matchingStates, int maxTickets, int validRange)}。
   */
  @Invoker
  static PointOfInterestType callRegister(String id, Set<BlockState> matchingStates, int maxTickets, int validRange) {
    throw new UnsupportedOperationException();
  }
}
