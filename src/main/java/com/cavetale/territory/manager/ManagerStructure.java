package com.cavetale.territory.manager;

import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.generator.structure.GeneratorStructureType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public final class ManagerStructure {
    private final GeneratorStructureType type;
    private final Structure structure;

    protected void enable() {
    }

    protected void disable() {
    }
}
