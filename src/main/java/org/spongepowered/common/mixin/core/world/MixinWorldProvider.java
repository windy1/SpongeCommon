/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import org.spongepowered.api.service.context.ServiceContext;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.interfaces.world.IMixinDimensionType;
import org.spongepowered.common.interfaces.world.IMixinWorldProvider;

@NonnullByDefault
@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider implements Dimension, IMixinWorldProvider {

    @Shadow public WorldType terrainType;
    @Shadow protected World worldObj;
    @Shadow public abstract net.minecraft.world.DimensionType getDimensionType();
    @Shadow public abstract boolean canRespawnHere();
    @Shadow public abstract int getAverageGroundLevel();
    @Shadow public abstract boolean doesWaterVaporize();
    @Shadow public abstract boolean getHasNoSky();
    @Shadow private String generatorSettings;

    @Override
    public DimensionType getType() {
        return (DimensionType) (Object) this.getDimensionType();
    }

    @Override
    public GeneratorType getGeneratorType() {
        return (GeneratorType) this.terrainType;
    }

    @Override
    public boolean allowsPlayerRespawns() {
        return this.canRespawnHere();
    }

    @Override
    public int getMinimumSpawnHeight() {
        return this.getAverageGroundLevel();
    }

    @Override
    public boolean doesWaterEvaporate() {
        return this.doesWaterVaporize();
    }

    @Override
    public boolean hasSky() {
        return !getHasNoSky();
    }

    @Override
    public void setGeneratorSettings(String generatorSettings) {
        this.generatorSettings = generatorSettings;
    }

    @Override
    public ServiceContext getContext() {
        return ((IMixinDimensionType) (Object) getDimensionType()).getContext();
    }
}
