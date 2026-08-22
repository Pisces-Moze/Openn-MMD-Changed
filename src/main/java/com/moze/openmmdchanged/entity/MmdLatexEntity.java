package com.moze.openmmdchanged.entity;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.TransfurMode;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** First hard-coded Changed form backed by a bundled MMD model. */
public final class MmdLatexEntity extends ChangedEntity {
    public MmdLatexEntity(EntityType<? extends MmdLatexEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public TransfurMode getTransfurMode() {
        return TransfurMode.REPLICATION;
    }
}
