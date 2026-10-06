package pers.solid.mishang.uc.block;

import pers.solid.mishang.uc.MishangUtils;
import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ModelProvider;
import net.minecraft.data.client.TextureKey;
import net.minecraft.data.client.TextureMap;
import net.minecraft.data.server.BlockLootTableGenerator;
import net.minecraft.data.server.recipe.CraftingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.SingleItemRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.loot.LootTable;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public interface MishangucBlock {
  default LootTable.Builder getLootTable(BlockLootTableGenerator blockLootTableGenerator) {
    return blockLootTableGenerator.drops((ItemConvertible) this);
  }

  default CraftingRecipeJsonBuilder getCraftingRecipe() {
    return null;
  }

  default SingleItemRecipeJsonBuilder getStonecuttingRecipe() {
    return null;
  }

  default Identifier getStonecuttingRecipeId() {
    return MishangUtils.withSuffixedPath(CraftingRecipeJsonBuilder.getItemId((ItemConvertible) this), "_from_stonecutting");
  }

  default boolean shouldWriteStonecuttingRecipe() {
    return false;
  }

  default void writeRecipes(Consumer<RecipeJsonProvider> exporter) {
    final CraftingRecipeJsonBuilder craftingRecipe = getCraftingRecipe();
    if (craftingRecipe != null) {
      // 1.18.2 的 ShapedRecipeJsonProvider 会读取产物物品的 ItemGroup；
      // 而本模组的物品是通过 Fabric 的 appendItems 加入分组、并未设置 Item.Settings.group，
      // 该值在数据生成阶段为 null 会触发 NPE。故此处显式指定配方分组名。
      final String group = customRecipeCategory();
      if (group != null) {
        craftingRecipe.group(group).offerTo(exporter);
      } else {
        craftingRecipe.group("mishanguc").offerTo(exporter);
      }
    }
    if (shouldWriteStonecuttingRecipe()) {
      final SingleItemRecipeJsonBuilder stonecuttingRecipe = getStonecuttingRecipe();
      if (stonecuttingRecipe != null) {
        stonecuttingRecipe.offerTo(exporter, getStonecuttingRecipeId());
      }
    }
  }

  void registerModels(ModelProvider modelProvider, BlockStateModelGenerator blockStateModelGenerator);


  default Identifier getTexture(TextureKey key) {
    return TextureMap.getId(((Block) this));
  }

  default String customRecipeCategory() {
    return null;
  }
}
