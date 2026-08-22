package com.moze.openmmdchanged.registry;

import com.moze.openmmdchanged.OpenMmdChanged;
import com.moze.openmmdchanged.entity.MmdLatexEntity;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTransfurVariants {
    public static final DeferredRegister<TransfurVariant<?>> REGISTRY =
            ChangedRegistry.TRANSFUR_VARIANT.createDeferred(OpenMmdChanged.MODID);

    public static final RegistryObject<TransfurVariant<MmdLatexEntity>> MMD_LATEX = REGISTRY.register(
            "mmd_latex",
            () -> TransfurVariant.Builder.of(ModEntities.MMD_LATEX).replicating().build());

    private ModTransfurVariants() {
    }
}
