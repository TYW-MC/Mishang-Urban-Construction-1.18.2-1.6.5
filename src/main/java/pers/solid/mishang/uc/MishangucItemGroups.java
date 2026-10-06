package pers.solid.mishang.uc;

import com.google.common.base.Preconditions;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.ObjectUtils;
import pers.solid.mishang.uc.blocks.*;
import pers.solid.mishang.uc.item.ExplosionToolItem;
import pers.solid.mishang.uc.item.FastBuildingToolItem;
import pers.solid.mishang.uc.item.MishangucItems;
import pers.solid.mishang.uc.util.ColorfulBlockRegistry;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 1.18.2 中 Fabric 的物品栏分组构建器是 {@link FabricItemGroupBuilder}（1.19+ 之后才改为
 * {@code net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup}），并且条目回调使用的是
 * {@code List<ItemStack>} 而非 {@code ItemGroup.Entries}。
 */
public class MishangucItemGroups {
  public static final ItemGroup ROADS = FabricItemGroupBuilder.create(Mishanguc.id("roads"))
      .icon(() -> new ItemStack(RoadBlocks.ROAD_WITH_WHITE_DOUBLE_LINE))
      .appendItems(stacks -> {
        MishangUtils.instanceStream(RoadBlocks.class, Block.class).forEach(addEntries(stacks));
        RoadSlabBlocks.SLABS.forEach(addEntries(stacks));
        MishangUtils.instanceStream(RoadMarkBlocks.class, Block.class).forEach(addEntries(stacks));
      })
      .build();

  public static final ItemGroup LIGHTS = FabricItemGroupBuilder.create(Mishanguc.id("lights"))
      .icon(() -> new ItemStack(LightBlocks.WHITE_LARGE_WALL_LIGHT))
      .appendItems(stacks -> MishangUtils.instanceStream(LightBlocks.class, Block.class).forEach(addEntries(stacks)))
      .build();

  public static final ItemGroup SIGNS = FabricItemGroupBuilder.create(Mishanguc.id("signs"))
      .icon(() -> new ItemStack(StandingSignBlocks.ACACIA_STANDING_SIGN))
      .appendItems(stacks -> {
        MishangUtils.instanceStream(WallSignBlocks.class, Block.class).forEach(addEntries(stacks));
        MishangUtils.instanceStream(HungSignBlocks.class, Block.class).forEach(addEntries(stacks));
        MishangUtils.instanceStream(StandingSignBlocks.class, Block.class).forEach(addEntries(stacks));
      })
      .build();

  public static final ItemGroup TOOLS = FabricItemGroupBuilder.create(Mishanguc.id("tools"))
      .icon(() -> new ItemStack(MishangucItems.ROTATING_TOOL))
      .appendItems(stacks -> MishangUtils.instanceStream(MishangucItems.class, ItemConvertible.class).forEach(item -> {
        if (item instanceof final ExplosionToolItem explosionToolItem) {
          explosionToolItem.appendToEntries(stacks);
        } else if (item instanceof final FastBuildingToolItem fastBuildingToolItem) {
          fastBuildingToolItem.appendToEntries(stacks);
        } else {
          stacks.add(item.asItem().getDefaultStack());
        }
      }))
      .build();

  public static final ItemGroup DECORATIONS = FabricItemGroupBuilder.create(Mishanguc.id("decorations"))
      .icon(() -> new ItemStack(HandrailBlocks.SIMPLE_ORANGE_CONCRETE_HANDRAIL))
      .appendItems(stacks -> MishangUtils.instanceStream(HandrailBlocks.class, Block.class).forEach(addEntries(stacks)))
      .build();

  public static final ItemGroup COLORED_BLOCKS = FabricItemGroupBuilder.create(Mishanguc.id("colored_blocks"))
      .icon(() -> new ItemStack(ColoredBlocks.COLORED_WOOL))
      .appendItems(stacks -> MishangUtils.instanceStream(ColoredBlocks.class, Block.class).forEach(addEntries(stacks)))
      .build();

  public static final List<DyeColor> FANCY_COLORS = List.of(
      DyeColor.WHITE,
      DyeColor.LIGHT_GRAY,
      DyeColor.GRAY,
      DyeColor.BLACK,
      DyeColor.BROWN,
      DyeColor.RED,
      DyeColor.ORANGE,
      DyeColor.YELLOW,
      DyeColor.LIME,
      DyeColor.GREEN,
      DyeColor.CYAN,
      DyeColor.LIGHT_BLUE,
      DyeColor.BLUE,
      DyeColor.PURPLE,
      DyeColor.MAGENTA,
      DyeColor.PINK
  );

  public static void init() {
    Preconditions.checkState(ObjectUtils.allNotNull(ROADS, LIGHTS, SIGNS, TOOLS, DECORATIONS, COLORED_BLOCKS));
  }

  private static <T extends Block> Consumer<T> addEntries(List<ItemStack> entries) {
    return t -> {
      if (ColorfulBlockRegistry.WHITE_TO_COLORFUL.containsKey(t)) {
        final Map<DyeColor, ? extends Block> map = ColorfulBlockRegistry.WHITE_TO_COLORFUL.get(t);
        for (DyeColor color : FANCY_COLORS) {
          if (map.containsKey(color)) {
            entries.add(map.get(color).asItem().getDefaultStack());
          }
        }
      } else if (!ColorfulBlockRegistry.COLORFUL_BLOCKS.contains(t)) {
        entries.add(t.asItem().getDefaultStack());
      }
    };
  }
}
