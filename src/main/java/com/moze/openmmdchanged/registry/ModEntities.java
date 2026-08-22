package com.moze.openmmdchanged.registry;

import com.moze.openmmdchanged.OpenMmdChanged;
import com.moze.openmmdchanged.entity.MmdLatexEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenMmdChanged.MODID);

    public static final RegistryObject<EntityType<MmdLatexEntity>> MMD_LATEX = REGISTRY.register(
            "mmd_latex",
            () -> EntityType.Builder.of(MmdLatexEntity::new, MobCategory.MONSTER)
                    .clientTrackingRange(10)
                    .sized(0.7F, 1.93F)
                    .build(OpenMmdChanged.id("mmd_latex").toString()));

    private ModEntities() {
    }

    public static AttributeSupplier.Builder createMmdLatexAttributes() {
        return ChangedEntity.createLatexAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 4.0D);
    }
}
