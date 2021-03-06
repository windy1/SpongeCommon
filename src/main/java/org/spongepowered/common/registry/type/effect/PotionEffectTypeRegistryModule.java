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
package org.spongepowered.common.registry.type.effect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import net.minecraft.potion.Potion;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.registry.AlternateCatalogRegistryModule;
import org.spongepowered.api.registry.util.AdditionalRegistration;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.common.mixin.core.potion.MixinPotion;
import org.spongepowered.common.registry.SpongeAdditionalCatalogRegistryModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PotionEffectTypeRegistryModule implements SpongeAdditionalCatalogRegistryModule<PotionEffectType>,
        AlternateCatalogRegistryModule<PotionEffectType> {

    public static PotionEffectTypeRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    private final List<PotionEffectType> potionList = new ArrayList<>();

    @RegisterCatalog(PotionEffectTypes.class)
    private final Map<String, PotionEffectType> potionEffectTypeMap = new HashMap<>();

    @Override
    public Map<String, PotionEffectType> provideCatalogMap() {
        Map<String, PotionEffectType> potionEffectTypeMap = new HashMap<>();
        for (Map.Entry<String, PotionEffectType> entry : this.potionEffectTypeMap.entrySet()) {
            potionEffectTypeMap.put(entry.getKey().replace("minecraft:", ""), entry.getValue());
        }
        return potionEffectTypeMap;
    }


    @Override
    public Optional<PotionEffectType> getById(String id) {
        if (!checkNotNull(id).contains(":")) {
            id = "minecraft:" + id; // assume vanilla
        }
        return Optional.ofNullable(this.potionEffectTypeMap.get(checkNotNull(id).toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public Collection<PotionEffectType> getAll() {
        return ImmutableList.copyOf(this.potionList);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerDefaults() {
        for (Potion potion : Potion.REGISTRY) {
            if (potion != null) {
                PotionEffectType potionEffectType = (PotionEffectType) potion;
                this.potionList.add(potionEffectType);
                this.potionEffectTypeMap.put(Potion.REGISTRY.getNameForObject(potion).toString(), potionEffectType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @AdditionalRegistration
    public void additionalRegistration() { // I'm guessing that this should work very well.
        for (Potion potion : Potion.REGISTRY) {
            if (potion != null) {
                PotionEffectType potionEffectType = (PotionEffectType) potion;
                if (!this.potionList.contains(potionEffectType)) {
                    this.potionList.add(potionEffectType);
                    this.potionEffectTypeMap.put(Potion.REGISTRY.getNameForObject(potion).toString(), potionEffectType);
                }
            }
        }
    }

    @Override
    public boolean allowsApiRegistration() {
        return false;
    }

    @Override
    public void registerAdditionalCatalog(PotionEffectType extraCatalog) {
    }

    public void registerFromGameData(String id, PotionEffectType itemType) {
        this.potionEffectTypeMap.put(id.toLowerCase(Locale.ENGLISH), itemType);
    }

    PotionEffectTypeRegistryModule() {

    }

    private static final class Holder {
        static final PotionEffectTypeRegistryModule INSTANCE = new PotionEffectTypeRegistryModule();
    }
}
