package pers.solid.mishang.uc.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class MishangucDataGeneration implements DataGeneratorEntrypoint {
  @Override
  public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
    // 1.18.2 的 FabricDataGenerator 没有 createPack()，直接 addProvider 即可。
    fabricDataGenerator.addProvider(MishangucBlockLootTableProvider::new);
    fabricDataGenerator.addProvider(MishangucRecipeProvider::new);
    fabricDataGenerator.addProvider(MishangucModelProvider::new);
    final MishangucBlockTagProvider blockTagProvider = fabricDataGenerator.addProvider(MishangucBlockTagProvider::new);
    fabricDataGenerator.addProvider(generator -> new MishangucItemTagProvider(generator, blockTagProvider));
  }
}
