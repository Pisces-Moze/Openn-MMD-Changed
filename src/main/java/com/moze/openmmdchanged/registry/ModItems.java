package com.moze.openmmdchanged.registry;

import com.moze.openmmdchanged.OpenMmdChanged;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, OpenMmdChanged.MODID);

    public static final RegistryObject<ForgeSpawnEggItem> MMD_LATEX_SPAWN_EGG = REGISTRY.register(
            "mmd_latex_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MMD_LATEX, 0x30343B, 0x7FD9FF, new Item.Properties()));

    private ModItems() {
    }
}
