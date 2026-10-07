package pers.solid.mishang.uc;

import com.google.common.base.Predicates;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import pers.solid.mishang.uc.block.AbstractRoadBlock;
import pers.solid.mishang.uc.block.ColoredBlock;
import pers.solid.mishang.uc.block.StandingSignBlock;
import pers.solid.mishang.uc.blockentity.*;
import pers.solid.mishang.uc.blocks.MishangucBlocks;
import pers.solid.mishang.uc.item.CarryingToolItem;
import pers.solid.mishang.uc.item.DataTagToolItem;
import pers.solid.mishang.uc.item.MishangucItems;
import pers.solid.mishang.uc.render.*;
import pers.solid.mishang.uc.screen.HungSignBlockEditScreen;
import pers.solid.mishang.uc.screen.SignPresets;
import pers.solid.mishang.uc.screen.StandingSignBlockEditScreen;
import pers.solid.mishang.uc.screen.WallSignBlockEditScreen;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Environment(EnvType.CLIENT)
public class MishangucClient implements ClientModInitializer {
  /**
   * @see MishangucRules#FORCE_PLACING_TOOL_ACCESS
   */
  public static final AtomicReference<MishangucRules.ToolAccess> CLIENT_FORCE_PLACING_TOOL_ACCESS = new AtomicReference<>(MishangucRules.ToolAccess.CREATIVE_ONLY);
  /**
   * @see MishangucRules#CARRYING_TOOL_ACCESS
   */
  public static final AtomicReference<MishangucRules.ToolAccess> CLIENT_CARRYING_TOOL_ACCESS = new AtomicReference<>(MishangucRules.ToolAccess.ALL);

  /** @see #scheduleForgeRenderLayers(java.util.List, java.util.List) */
  private static boolean forgeRenderLayersApplied = false;

  @Override
  public void onInitializeClient() {
    registerBlockLayers();

    registerRenderEvents();

    registerBlockEntityRenderers();

    registerBlockColors();

    registerNetworking();

    registerModelPredicateProviders();
  }

  private static void registerModelPredicateProviders() {
    // 模型谓词提供器
    ModelPredicateProviderRegistry.register(MishangucItems.EXPLOSION_TOOL,
        Mishanguc.id("explosion_power"),
        new UnclampedModelPredicateProvider() {
          @Override
          public float unclampedCall(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
            return MishangucItems.EXPLOSION_TOOL.power(stack);
          }
        });

    SignPresets.loadAll();

    SignPresetCommand.INSTANCE.init();

    ModelPredicateProviderRegistry.register(MishangucItems.EXPLOSION_TOOL, Mishanguc.id("explosion_create_fire"), (stack, world, entity, seed) -> MishangucItems.EXPLOSION_TOOL.createFire(stack) ? 1 : 0);
    ModelPredicateProviderRegistry.register(MishangucItems.FAST_BUILDING_TOOL, Mishanguc.id("fast_building_range"), (stack, world, entity, seed) -> MishangucItems.FAST_BUILDING_TOOL.getRange(stack) / 64f);
    ModelPredicateProviderRegistry.register(MishangucItems.CARRYING_TOOL, Mishanguc.id("is_holding_block"), (stack, world, entity, seed) -> BooleanUtils.toInteger(CarryingToolItem.hasHoldingBlockState(stack)));
    ModelPredicateProviderRegistry.register(MishangucItems.CARRYING_TOOL, Mishanguc.id("is_holding_entity"), (stack, world, entity, seed) -> BooleanUtils.toInteger(CarryingToolItem.hasHoldingEntity(stack)));
  }

  private static void registerNetworking() {
    // 网络通信
    // 客户端收到服务器发来的编辑告示牌的数据包时，打开编辑界面，允许用户编辑。
    ClientPlayNetworking.registerGlobalReceiver(
        new Identifier("mishanguc", "edit_sign"),
        (client, handler, buf, responseSender) -> {
          try {
            final BlockPos blockPos = buf.readBlockPos();
            final BlockEntity blockEntity =
                client.world != null ? client.world.getBlockEntity(blockPos) : null;
            if (blockEntity instanceof final HungSignBlockEntity hungSignBlockEntity) {
              final Direction direction = buf.readEnumConstant(Direction.class);
              client.execute(() ->
                  client.setScreen(new HungSignBlockEditScreen(hungSignBlockEntity, direction, blockPos)));
            } else if (blockEntity instanceof final WallSignBlockEntity wallSignBlockEntity) {
              client.execute(() ->
                  client.setScreen(new WallSignBlockEditScreen(wallSignBlockEntity, blockPos)));
            } else if (blockEntity instanceof final StandingSignBlockEntity standingSignBlockEntity) {
              final BlockHitResult blockHitResult = buf.readBlockHitResult();
              final Boolean isFront = StandingSignBlock.getHitSide(blockEntity.getCachedState(), blockHitResult);
              if (isFront != null) {
                client.execute(() -> client.setScreen(new StandingSignBlockEditScreen(standingSignBlockEntity, blockPos, isFront)));
              }
            }
          } catch (NullPointerException | ClassCastException exception) {
            Mishanguc.MISHANG_LOGGER.error("Error when creating sign edit screen:", exception);
          }
        });
    ClientPlayNetworking.registerGlobalReceiver(new Identifier("mishanguc", "get_block_data"), new DataTagToolItem.BlockDataReceiver());
    ClientPlayNetworking.registerGlobalReceiver(new Identifier("mishanguc", "get_entity_data"), new DataTagToolItem.EntityDataReceiver());
    ClientPlayNetworking.registerGlobalReceiver(new Identifier("mishanguc", "rule_changed"), MishangucRules::handle);
  }

  private static void registerBlockColors() {
    // 注册方块和颜色
    final Block[] coloredBlocks = MishangUtils.blocks().stream().filter(Predicates.instanceOf(ColoredBlock.class))
        .toArray(Block[]::new);
    ColorProviderRegistry.BLOCK.register(
        (state, world, pos, tintIndex) -> {
          if (world == null || pos == null) return -1;
          BlockEntity entity = world.getBlockEntity(pos);
          // 考虑到玩家掉落产生粒子时，坐标会向上偏离一格。
          if (entity == null) entity = world.getBlockEntity(pos.down());
          if (entity instanceof ColoredBlockEntity coloredBlockEntity) {
            return coloredBlockEntity.getColor();
          } else {
            // 考虑到坐标本身的位置没有方块颜色，因此根据附近坐标来推断方块颜色。
            // 受部分渲染器影响，方块颜色会与周围插值，故需确保有自定义颜色的方块周围也会带有相同的自定义颜色。
            int accumulatedNum = 0;
            int accumulatedRed = 0;
            int accumulatedGreen = 0;
            int accumulatedBlue = 0;
            for (BlockPos outPos : BlockPos.iterateOutwards(pos, 1, 1, 1)) {
              if (outPos.equals(pos)) continue;
              if (world.getBlockEntity(outPos) instanceof ColoredBlockEntity coloredBlockEntity) {
                final int color = coloredBlockEntity.getColor();
                accumulatedNum += 1;
                accumulatedRed += color >> 16 & 255;
                accumulatedGreen += color >> 8 & 255;
                accumulatedBlue += color & 255;
              }
            }
            if (accumulatedNum > 0) {
              return (accumulatedRed / accumulatedNum << 16) + (accumulatedGreen / accumulatedNum << 8) + accumulatedBlue / accumulatedNum;
            } else {
              return -1;
            }
          }
        },
        coloredBlocks
    );
    ColorProviderRegistry.ITEM.register(
        (stack, tintIndex) -> {
          final NbtCompound nbt = stack.getSubNbt("BlockEntityTag");
          if (nbt != null && nbt.contains("color", NbtElement.NUMBER_TYPE)) {
            return 0xff000000 | nbt.getInt("color");
          }
          return Color.HSBtoRGB(Util.getMeasuringTimeMs() / 4096f + (stack.getItem().hashCode() >> 16) / 64f, 0.5f, 0.95f);
        },
        coloredBlocks
    );
  }

  private static void registerBlockEntityRenderers() {
    // 注册方块实体渲染器
    BlockEntityRendererRegistry.register(MishangucBlockEntities.HUNG_SIGN_BLOCK_ENTITY, HungSignBlockEntityRenderer::new);
    BlockEntityRendererRegistry.register(MishangucBlockEntities.COLORED_HUNG_SIGN_BLOCK_ENTITY, HungSignBlockEntityRenderer::new);
    BlockEntityRendererRegistry.register(MishangucBlockEntities.WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
    BlockEntityRendererRegistry.register(MishangucBlockEntities.COLORED_WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
    BlockEntityRendererRegistry.register(MishangucBlockEntities.FULL_WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer<FullWallSignBlockEntity>::new);
    BlockEntityRendererRegistry.register(MishangucBlockEntities.STANDING_SIGN_BLOCK_ENTITY, StandingSignBlockEntityRenderer::new);
    BlockEntityRendererRegistry.register(MishangucBlockEntities.COLORED_STANDING_SIGN_BLOCK_ENTITY, StandingSignBlockEntityRenderer::new);
  }

  private static void registerRenderEvents() {
    // 注册方块外观描绘
    WorldRenderEvents.BLOCK_OUTLINE.register(RendersBlockOutline.RENDERER);
    WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(RendersBeforeOutline.RENDERER);
  }

  private static void registerBlockLayers() {
    // 设置相应的 BlockLayer
    final List<Block> translucentList = new java.util.ArrayList<>();
    final List<Block> cutoutList = new java.util.ArrayList<>();
    Validate.notEmpty(MishangucBlocks.translucentBlocks).forEach(block -> {
      BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent());
      translucentList.add(block);
    });
    Validate.notEmpty(MishangucBlocks.cutoutBlocks).forEach(block -> {
      BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
      cutoutList.add(block);
      if (block instanceof AbstractRoadBlock roadBlock && roadBlock.getRoadSlab() != null) {
        BlockRenderLayerMap.INSTANCE.putBlock(roadBlock.getRoadSlab(), RenderLayer.getCutout());
        cutoutList.add(roadBlock.getRoadSlab());
      }
    });
    scheduleForgeRenderLayers(cutoutList, translucentList);
    MishangucBlocks.translucentBlocks = null;
    MishangucBlocks.cutoutBlocks = null;
  }

  /**
   * <p>把 Forge 侧的渲染层补注册推迟到客户端的第一个 tick。
   *
   * <p>Sinytra Connector 下 Fabric 的客户端入口点由 {@code Main#main} 直接调用，此时
   * {@code MinecraftClient} 还没有被构造出来。而 Embeddium / Rubidium 混入了 Forge 的
   * {@code ItemBlockRenderTypes.setRenderLayer}，其回调里会执行
   * {@code Minecraft.getInstance().execute(...)} —— 实例为 null 时直接抛
   * {@link NullPointerException}。由于
   * {@link #registerForgeRenderLayers(Iterable, Iterable)} 还会顺带触发
   * {@code ItemBlockRenderTypes} 的类初始化，这个 NPE 会以
   * {@code ExceptionInInitializerError} 的形式让整个类初始化失败，客户端在启动阶段
   * 就崩溃（日志表现为 Connector 报 {@code Could not execute entrypoint stage
   * 'client' ... provided by 'mishanguc'}）。
   *
   * <p>第一个 tick 时客户端对象已经存在，又远早于任何区块渲染，因此既保住了对
   * Embeddium 一类优化模组的适配，也不会再踩到这个雷。
   */
  private static void scheduleForgeRenderLayers(List<Block> cutoutBlocks, List<Block> translucentBlocks) {
    ClientTickEvents.END_CLIENT_TICK.register(client -> {
      if (forgeRenderLayersApplied) {
        return;
      }
      forgeRenderLayersApplied = true;
      registerForgeRenderLayers(cutoutBlocks, translucentBlocks);
    });
  }

  /**
   * <p>Forge（含 Sinytra Connector）环境兼容。Forge 同时存在两套方块渲染层映射：
   * 原版的 {@code TYPE_BY_BLOCK}（{@code f_109275_}，区块渲染
   * {@code getChunkRenderType} 路径），以及 Forge 自己的 {@code blockRenderChecks}
   * 谓词映射（{@code canRenderInLayer} / {@code getRenderType(state, cull)} 路径）。
   * Fabric API 的 {@code BlockRenderLayerMap} 经 Connector 桥接时只写入前者，
   * 而部分优化模组（如 Embeddium/Rubidium）查询的是谓词映射，于是本模组带透明
   * 贴图的方块（道路标线、地面标识、告示牌等）被按 solid 层渲染，透明像素
   * （RGB 为黑或白）被直接画出来，外观呈现为黑色或白色色块。
   *
   * <p>这里在检测到 Forge 运行环境时，通过反射调用 {@code setRenderLayer} 写入
   * 谓词映射，并直接写入原版 {@code TYPE_BY_BLOCK}，两套映射双保险。纯 Fabric
   * 环境没有 {@code net.minecraft.client.renderer.ItemBlockRenderTypes} 类，
   * 直接跳过。渲染层实例本身（{@link RenderLayer#getCutout()} 等）由 Connector
   * 重映射到 Forge 运行时的同一单例，可直接复用。
   *
   * <p><b>调用时机：</b>必须等到 MinecraftClient 实例存在之后才能调用，否则
   * Embeddium 一类模组对 {@code setRenderLayer} 的挂钩会因
   * {@code Minecraft.getInstance()} 为 null 而让 {@code ItemBlockRenderTypes}
   * 类初始化失败、客户端直接崩溃。因此本方法只应通过
   * {@link #scheduleForgeRenderLayers(List, List)} 间接调用。
   */
  @SuppressWarnings("unchecked")
  private static void registerForgeRenderLayers(Iterable<Block> cutoutBlocks, Iterable<Block> translucentBlocks) {
    final Class<?> clazz;
    try {
      clazz = Class.forName("net.minecraft.client.renderer.ItemBlockRenderTypes");
    } catch (ClassNotFoundException e) {
      // 纯 Fabric 环境，无需 Forge 兼容。
      return;
    }
    try {
      // 1) Forge 的谓词映射：优化模组（Embeddium 等）经 canRenderInLayer 查询的路径。
      final Method setRenderLayer = clazz.getMethod("setRenderLayer", Block.class, RenderLayer.class);
      for (final Block block : cutoutBlocks) {
        setRenderLayer.invoke(null, block, RenderLayer.getCutout());
      }
      for (final Block block : translucentBlocks) {
        setRenderLayer.invoke(null, block, RenderLayer.getTranslucent());
      }
      // 2) 原版的 TYPE_BY_BLOCK 映射：区块渲染路径，字段在 1.18.2 Forge 中为 f_109275_。
      Field typeByBlock = null;
      for (final String fieldName : new String[] {"TYPE_BY_BLOCK", "f_109275_"}) {
        try {
          typeByBlock = clazz.getDeclaredField(fieldName);
          break;
        } catch (NoSuchFieldException ignored) {
        }
      }
      if (typeByBlock != null) {
        typeByBlock.setAccessible(true);
        final Map<Block, RenderLayer> map = (Map<Block, RenderLayer>) typeByBlock.get(null);
        for (final Block block : cutoutBlocks) {
          map.put(block, RenderLayer.getCutout());
        }
        for (final Block block : translucentBlocks) {
          map.put(block, RenderLayer.getTranslucent());
        }
      }
      Mishanguc.MISHANG_LOGGER.info(
          "Applied Forge render-layer compatibility (Embeddium etc.): {} cutout blocks, {} translucent blocks.",
          cutoutListSize(cutoutBlocks), cutoutListSize(translucentBlocks));
    } catch (final ReflectiveOperationException | RuntimeException e) {
      Mishanguc.MISHANG_LOGGER.error("Failed to apply Forge render-layer compatibility.", e);
    }
  }

  private static int cutoutListSize(Iterable<Block> blocks) {
    int size = 0;
    for (final Block ignored : blocks) {
      size++;
    }
    return size;
  }
}
