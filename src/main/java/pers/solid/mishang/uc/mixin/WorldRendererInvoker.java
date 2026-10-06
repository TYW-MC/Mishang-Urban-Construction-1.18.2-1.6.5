package pers.solid.mishang.uc.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public interface WorldRendererInvoker {
  /**
   * 在指定位置渲染指定外观。
   * <p>
   * 1.18.2 中对应的方法是 {@code drawShapeOutline}（1.19+ 改名为 {@code drawCuboidShapeOutline}）。
   *
   * @see WorldRenderer
   */
  @Invoker("drawShapeOutline")
  static void drawShapeOutline(
      MatrixStack matrices,
      VertexConsumer vertexConsumer,
      VoxelShape shape,
      double offsetX,
      double offsetY,
      double offsetZ,
      float red,
      float green,
      float blue,
      float alpha) {
    throw new AssertionError();
  }
}
