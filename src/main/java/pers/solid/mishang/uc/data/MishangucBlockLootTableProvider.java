package pers.solid.mishang.uc.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.util.Identifier;
import pers.solid.mishang.uc.MishangUtils;
import pers.solid.mishang.uc.block.MishangucBlock;

public class MishangucBlockLootTableProvider extends FabricBlockLootTableProvider {
  protected MishangucBlockLootTableProvider(FabricDataGenerator dataGenerator) {
    super(dataGenerator);
  }

  @Override
  protected void generateBlockLootTables() {
    for (Block block : MishangUtils.blocks()) {
      if (block instanceof MishangucBlock r) {
        final Identifier lootTableId = block.getLootTableId();
        if (LootTables.EMPTY.equals(lootTableId)) {
          continue;
        }
        // 1.18.2 中 lootTables 字段为 private，使用 addDrop(Block, LootTable.Builder) 注册。
        final LootTable.Builder lootTable = r.getLootTable(this);
        addDrop(block, lootTable);
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
