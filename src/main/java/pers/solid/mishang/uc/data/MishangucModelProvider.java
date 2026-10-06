package pers.solid.mishang.uc.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.block.MishangucBlock;
import pers.solid.mishang.uc.blocks.WallSignBlocks;
import pers.solid.mishang.uc.item.MishangucItems;
import pers.solid.mishang.uc.mixin.ItemModelGeneratorAccessor;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MishangucModelProvider extends FabricModelProvider {
  public MishangucModelProvider(FabricDataGenerator output) {
    super(output);
  }

  @Override
  public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
    MishangUtils.blocks().forEach(block -> {
      if (block instanceof MishangucBlock mishangucBlock) {
        mishangucBlock.registerModels(this, blockStateModelGenerator);
      }
    });
  }

  @Override
  public void generateItemModels(ItemModelGenerator itemModelGenerator) {
    itemModelGenerator.register(WallSignBlocks.INVISIBLE_WALL_SIGN.asItem(), Models.HANDHELD);
    itemModelGenerator.register(WallSignBlocks.INVISIBLE_GLOWING_WALL_SIGN.asItem(), Models.HANDHELD);

    itemModelGenerator.register(MishangucItems.BLOCK_STATE_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.FLUID_STATE_TOOL, Models.HANDHELD);
    registerCarryingTool(itemModelGenerator, MishangucItems.CARRYING_TOOL);
    itemModelGenerator.register(MishangucItems.COLOR_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.COLUMN_BUILDING_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.DATA_TAG_TOOL, Models.HANDHELD);
    registerExplosionToolVariants(itemModelGenerator, MishangucItems.EXPLOSION_TOOL);
    itemModelGenerator.register(MishangucItems.FORCE_PLACING_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.FLUID_FORCE_PLACING_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.GROWTH_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.ICE_SNOW_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.ID_CHECKER_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.FLUID_ID_CHECKER_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.MIRRORING_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.OMNIPOTENT_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.ROAD_CONNECTION_STATE_DEBUGGING_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.ROAD_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.ROTATING_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.SLAB_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.TEXT_COPY_TOOL, Models.HANDHELD);
    itemModelGenerator.register(MishangucItems.TP_TOOL, Models.HANDHELD);
  }

  private void registerCarryingTool(ItemModelGenerator itemModelGenerator, Item item) {
    // 1.18.2 中 ItemModelGenerator.writer 为 private，通过访问器取得输出回调；
    // Model 也没有公开的 createJson，因此手写 handheld 模板并附加 overrides。
    final BiConsumer<Identifier, Supplier<JsonElement>> writer =
        ((ItemModelGeneratorAccessor) itemModelGenerator).getWriter();

    itemModelGenerator.register(item, "_with_block", Models.HANDHELD);
    itemModelGenerator.register(item, "_with_entity", Models.HANDHELD);

    writer.accept(
        ModelIds.getItemModelId(item),
        () -> {
          final JsonObject json = new JsonObject();
          json.addProperty("parent", "item/handheld");
          final JsonObject textures = new JsonObject();
          textures.addProperty("layer0", ModelIds.getItemModelId(item).toString());
          json.add("textures", textures);
          final JsonArray overrides = new JsonArray();
          json.add("overrides", overrides);
          final JsonObject withBlock = new JsonObject();
          overrides.add(withBlock);
          final JsonObject withBlockPredicate = new JsonObject();
          withBlock.add("predicate", withBlockPredicate);
          withBlockPredicate.addProperty("mishanguc:is_holding_block", 1f);
          withBlock.addProperty("model", ModelIds.getItemSubModelId(item, "_with_block").toString());
          final JsonObject withEntity = new JsonObject();
          overrides.add(withEntity);
          final JsonObject withEntityPredicate = new JsonObject();
          withEntity.add("predicate", withEntityPredicate);
          withEntityPredicate.addProperty("mishanguc:is_holding_entity", 1f);
          withEntity.addProperty("model", ModelIds.getItemSubModelId(item, "_with_entity").toString());
          return json;
        });
  }

  private void registerExplosionToolVariants(ItemModelGenerator itemModelGenerator, Item item) {
    for (final String name : new String[]{
        "_fire",
        "_4", "_4_fire",
        "_8", "_8_fire",
        "_16", "_16_fire",
        "_32", "_32_fire",
        "_64", "_64_fire",
        "_128", "_128_fire",
    }) {
      itemModelGenerator.register(item, name, Models.HANDHELD);
    }
  }
}
