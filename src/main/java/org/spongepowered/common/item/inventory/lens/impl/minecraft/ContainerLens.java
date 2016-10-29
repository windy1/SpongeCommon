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
package org.spongepowered.common.item.inventory.lens.impl.minecraft;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.comp.GridInventoryLens;
import org.spongepowered.common.item.inventory.lens.comp.HotbarLens;
import org.spongepowered.common.item.inventory.lens.impl.MinecraftLens;
import org.spongepowered.common.item.inventory.lens.impl.comp.GridInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.HotbarLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.comp.OrderedInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.slots.SlotLensImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerLens extends MinecraftLens {

    // The container for reference
    private Container container;
    // The viewed inventories
    private List<Lens<IInventory, ItemStack>> inventories = new ArrayList<>();
    // In vanilla containers display the player inventory + hotbar
    private GridInventoryLens player;
    private HotbarLens hotbar;


    public ContainerLens(Container container, InventoryAdapter<IInventory, ItemStack> adapter, SlotProvider<IInventory, ItemStack> slots) {
        super(0, adapter.getInventory().getSize(), adapter, slots);
        this.container = container;
        this.init(slots);
    }

    @Override
    protected void init(SlotProvider<IInventory, ItemStack> slots) {
        System.out.print("### ContainerLens for: " + container.getClass().getName() + " ###\n");
        // Get all inventories viewed in the Container & count slots & retain order
        Map<IInventory, Long> viewed = container.inventorySlots.stream()
                .map(slot -> slot.inventory)
                .collect(Collectors.groupingBy(i -> i, LinkedHashMap::new, Collectors.counting()));
        int index = 0;
        for (Map.Entry<IInventory, Long> entry : viewed.entrySet()) {
            Lens<IInventory, ItemStack> lens = null;
            System.out.print(" - " + entry.getKey().getClass().getName() + " (" + entry.getValue() + ")\t");
            if (entry.getKey() instanceof InventoryAdapter) {
                lens = ((InventoryAdapter) entry.getKey()).getRootLens();
            }
            if (lens == null // Unknown Inventory or
                    || lens.slotCount() != entry.getValue()) { // Inventory size <> Lens size
                // TODO PlayerInventoryLens(41) has more slots than the displayed inventory+hotbar(36)
                if (entry.getValue() == 1) {
                    System.out.print("???");
                    lens = new SlotLensImpl(index);
                } else {
                    if (lens instanceof PlayerInventoryLens && entry.getValue() == 36) {
                        // Player Inventory + Hotbar
                        System.out.print("Player GridLens (27) + HotbarLens (9)\n");
                        this.player = new GridInventoryLensImpl(index, 9, 3, 9, slots);
                        this.hotbar = new HotbarLensImpl(index + 27, 9, slots);
                        lens = null;
                    }
                    else
                    {
                        System.out.print("???");
                        lens = new OrderedInventoryLensImpl(index, entry.getValue().intValue(), 1, slots);
                    }
                    // TODO try grid?
                }
            }
            if (lens != null) {
                System.out.print(lens.getClass().getSimpleName() + " (" + lens.slotCount() + ")\n");
                this.inventories.add(lens);
                this.addSpanningChild(lens);
            }

            index += entry.getValue();
        }
        System.out.print("#########################################################\n");
    }

    @Override
    protected boolean isDelayedInit() {
        return true; // We need the container for init
    }
}
