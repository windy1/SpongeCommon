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
package org.spongepowered.common.event.tracking.phase.general;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.tracking.CapturedMultiMapSupplier;
import org.spongepowered.common.event.tracking.CapturedSupplier;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.ItemDropData;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.TrackingPhase;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.world.IMixinLocation;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.world.BlockChange;
import org.spongepowered.common.world.SpongeProxyBlockAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public final class GeneralPhase extends TrackingPhase {

    public static final class State {
        public static final IPhaseState COMMAND = new CommandState();
        public static final IPhaseState EXPLOSION = new ExplosionState();
        public static final IPhaseState COMPLETE = new CompletePhase();

        private State() { }
    }

    public static final class Post {
        public static final IPhaseState UNWINDING = new PostState();

        private Post() { }
    }


    public static GeneralPhase getInstance() {
        return Holder.INSTANCE;
    }

    private GeneralPhase() {
    }

    private static final class Holder {
        static final GeneralPhase INSTANCE = new GeneralPhase();
    }

    @Override
    public void unwind(CauseTracker causeTracker, IPhaseState state, PhaseContext phaseContext) {
        ((GeneralState) state).unwind(causeTracker, phaseContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void postDispatch(CauseTracker causeTracker, IPhaseState unwindingState, PhaseContext unwindingContext, PhaseContext postContext) {
        final List<BlockSnapshot> contextBlocks = postContext.getCapturedBlockSupplier().orEmptyList();
        final List<Entity> contextEntities = postContext.getCapturedEntitySupplier().orEmptyList();
        final List<Entity> contextItems = (List<Entity>) (List<?>) postContext.getCapturedItemsSupplier().orEmptyList();
        if (contextBlocks.isEmpty() && contextEntities.isEmpty() && contextItems.isEmpty()) {
            return;
        }
        if (!contextBlocks.isEmpty()) {
            final List<BlockSnapshot> blockSnapshots = new ArrayList<>(contextBlocks);
            contextBlocks.clear();
            processBlockTransactionListsPost(postContext, blockSnapshots, causeTracker, unwindingState, unwindingContext);
        }
        if (!contextEntities.isEmpty()) {
            final ArrayList<Entity> entities = new ArrayList<>(contextEntities);
            contextEntities.clear();
            unwindingState.getPhase().processPostEntitySpawns(causeTracker, unwindingState, unwindingContext, entities);
        }
        if (!contextItems.isEmpty()) {
            final ArrayList<Entity> items = new ArrayList<>(contextItems);
            contextItems.clear();
            unwindingState.getPhase().processPostItemSpawns(causeTracker, unwindingState, items);
        }

    }

    /**
     *
     * @param snapshotsToProcess
     * @param causeTracker
     * @param unwindingState
     * @param unwinding
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void processBlockTransactionListsPost(PhaseContext postContext, List<BlockSnapshot> snapshotsToProcess, CauseTracker causeTracker,
            IPhaseState unwindingState, PhaseContext unwinding) {
        final List<Transaction<BlockSnapshot>> invalidTransactions = new ArrayList<>();
        ImmutableList<Transaction<BlockSnapshot>>[] transactionArrays = new ImmutableList[TrackingUtil.EVENT_COUNT];
        ImmutableList.Builder<Transaction<BlockSnapshot>>[] transactionBuilders = new ImmutableList.Builder[TrackingUtil.EVENT_COUNT];
        for (int i = 0; i < TrackingUtil.EVENT_COUNT; i++) {
            transactionBuilders[i] = new ImmutableList.Builder<>();
        }
        final List<ChangeBlockEvent> blockEvents = new ArrayList<>();
        final WorldServer minecraftWorld = causeTracker.getMinecraftWorld();

        for (BlockSnapshot snapshot : snapshotsToProcess) {
            // This processes each snapshot to assign them to the correct event in the next area, with the
            // correct builder array entry.
            TrackingUtil.TRANSACTION_PROCESSOR.apply(transactionBuilders)
                    .accept(TrackingUtil.TRANSACTION_CREATION.apply(minecraftWorld, snapshot));
        }

        for (int i = 0; i < TrackingUtil.EVENT_COUNT; i++) {
            // Build each event array
            transactionArrays[i] = transactionBuilders[i].build();
        }
        final ChangeBlockEvent[] mainEvents = new ChangeBlockEvent[BlockChange.values().length];
        // This likely needs to delegate to the phase in the event we don't use the source object as the main object causing the block changes
        // case in point for WorldTick event listeners since the players are captured non-deterministically
        final Cause.Builder builder = Cause.source(unwinding.getSource(Object.class).get());
        final World world = causeTracker.getWorld();
        // Creates the block events accordingly to the transaction arrays
        TrackingUtil.iterateChangeBlockEvents(transactionArrays, blockEvents, mainEvents, builder, world);
        // We create the post event and of course post it in the method, regardless whether any transactions are invalidated or not
        final ChangeBlockEvent.Post
                postEvent =
                TrackingUtil.throwMultiEventsAndCreatePost(transactionArrays, blockEvents, mainEvents, builder, world);

        if (postEvent == null) { // Means that we have had no actual block changes apparently?
            return;
        }

        // Iterate through the block events to mark any transactions as invalid to accumilate after (since the post event contains all
        // transactions of the preceeding block events)
        for (ChangeBlockEvent blockEvent : blockEvents) { // Need to only check if the event is cancelled, If it is, restore
            if (blockEvent.isCancelled()) {
                // Don't restore the transactions just yet, since we're just marking them as invalid for now
                for (Transaction<BlockSnapshot> transaction : Lists.reverse(blockEvent.getTransactions())) {
                    transaction.setValid(false);
                }
            }
        }

        // Finally check the post event
        if (postEvent.isCancelled()) {
            // Of course, if post is cancelled, just mark all transactions as invalid.
            for (Transaction<BlockSnapshot> transaction : postEvent.getTransactions()) {
                transaction.setValid(false);
            }
        }

        // Now we can gather the invalid transactions that either were marked as invalid from an event listener - OR - cancelled.
        // Because after, we will restore all the invalid transactions in reverse order.
        for (Transaction<BlockSnapshot> transaction : postEvent.getTransactions()) {
            if (!transaction.isValid()) {
                invalidTransactions.add(transaction);
            }
        }

        if (!invalidTransactions.isEmpty()) {
            // NOW we restore the invalid transactions (remember invalid transactions are from either plugins marking them as invalid
            // or the events were cancelled), again in reverse order of which they were received.
            for (Transaction<BlockSnapshot> transaction : Lists.reverse(invalidTransactions)) {
                transaction.getOriginal().restore(true, BlockChangeFlag.NONE);
                if (unwindingState.tracksBlockSpecificDrops()) {
                    // Cancel any block drops or harvests for the block change.
                    // This prevents unnecessary spawns.
                    final BlockPos position = ((IMixinLocation) (Object) transaction.getOriginal().getLocation().get()).getBlockPos();
                    postContext.getBlockDropSupplier().ifPresentAndNotEmpty(map -> {
                        if (map.containsKey(position)) {
                            map.get(position).clear();
                        }
                    });
                }
            }
            invalidTransactions.clear();
        }
        performPostBlockAdditions(causeTracker, postContext, postEvent.getTransactions(), builder, unwindingState, unwinding);
    }

    private static void performPostBlockAdditions(CauseTracker causeTracker, PhaseContext postContext, List<Transaction<BlockSnapshot>> transactions,
            Cause.Builder builder, IPhaseState unwindingState, PhaseContext unwindingPhaseContext) {
        // We have to use a proxy so that our pending changes are notified such that any accessors from block
        // classes do not fail on getting the incorrect block state from the IBlockAccess
        final WorldServer minecraftWorld = causeTracker.getMinecraftWorld();
        final SpongeProxyBlockAccess proxyBlockAccess = new SpongeProxyBlockAccess(minecraftWorld, transactions);
        final CapturedMultiMapSupplier<BlockPos, ItemDropData> capturedBlockDrops = postContext.getBlockDropSupplier();
        final CapturedMultiMapSupplier<BlockPos, EntityItem> capturedBlockItemEntityDrops = postContext.getBlockItemDropSupplier();
        for (Transaction<BlockSnapshot> transaction : transactions) {
            if (!transaction.isValid()) {
                continue; // Don't use invalidated block transactions during notifications, these only need to be restored
            }
            // Handle custom replacements
            if (transaction.getCustom().isPresent()) {
                transaction.getFinal().restore(true, BlockChangeFlag.ALL);
            }

            final SpongeBlockSnapshot oldBlockSnapshot = (SpongeBlockSnapshot) transaction.getOriginal();
            final SpongeBlockSnapshot newBlockSnapshot = (SpongeBlockSnapshot) transaction.getFinal();

            // Handle item drops captured
            final BlockPos pos = ((IMixinLocation) (Object) oldBlockSnapshot.getLocation().get()).getBlockPos();
            capturedBlockDrops.ifPresentAndNotEmpty(map -> TrackingUtil
                    .spawnItemDataForBlockDrops(map.containsKey(pos) ? map.get(pos) : Collections.emptyList(), newBlockSnapshot, causeTracker, unwindingPhaseContext, unwindingState));
            capturedBlockItemEntityDrops.ifPresentAndNotEmpty(map -> TrackingUtil
                    .spawnItemEntitiesForBlockDrops(map.containsKey(pos) ? map.get(pos) : Collections.emptyList(), newBlockSnapshot, causeTracker, unwindingPhaseContext, unwindingState));

            SpongeHooks.logBlockAction(builder, minecraftWorld, oldBlockSnapshot.blockChange, transaction);
            final BlockChangeFlag changeFlag = oldBlockSnapshot.getChangeFlag();
            final int updateFlag = oldBlockSnapshot.getUpdateFlag();
            final IBlockState originalState = (IBlockState) oldBlockSnapshot.getState();
            final IBlockState newState = (IBlockState) newBlockSnapshot.getState();
            // Containers get placed automatically
            final CapturedSupplier<BlockSnapshot> capturedBlockSupplier = postContext.getCapturedBlockSupplier();
            if (changeFlag.performBlockPhysics() && originalState.getBlock() != newState.getBlock() && !SpongeImplHooks.blockHasTileEntity(newState.getBlock(), newState)) {
                newState.getBlock().onBlockAdded(minecraftWorld, pos, newState);
                postContext.getCapturedEntitySupplier().ifPresentAndNotEmpty(entities -> {

                });
                capturedBlockSupplier.ifPresentAndNotEmpty(blocks -> {
                    final List<BlockSnapshot> blockSnapshots = new ArrayList<>(blocks);
                    blocks.clear();
                    processBlockTransactionListsPost(postContext, blockSnapshots, causeTracker, unwindingState, unwindingPhaseContext);
                });
            }

            proxyBlockAccess.proceed();

            unwindingState.handleBlockChangeWithUser(oldBlockSnapshot.blockChange, minecraftWorld, transaction, unwindingPhaseContext);

            if (((updateFlag & 2) != 0)) {
                // Since notifyBlockUpdate is basically to tell clients that the block position has changed,
                // we need to respect that flag
                causeTracker.getMinecraftWorld().notifyBlockUpdate(pos, originalState, newState, updateFlag);
            }

            if (changeFlag.updateNeighbors()) { // Notify neighbors only if the change flag allowed it.
                causeTracker.getMixinWorld().spongeNotifyNeighborsPostBlockChange(pos, originalState, newState, oldBlockSnapshot.getUpdateFlag());
            }
            capturedBlockSupplier.ifPresentAndNotEmpty(blocks -> {
                final List<BlockSnapshot> blockSnapshots = new ArrayList<>(blocks);
                blocks.clear();
                processBlockTransactionListsPost(postContext, blockSnapshots, causeTracker, unwindingState, unwindingPhaseContext);
            });
        }
    }

    @Override
    public boolean ignoresBlockUpdateTick(PhaseData phaseData) {
        return phaseData.state == Post.UNWINDING;
    }

    @Override
    public boolean ignoresBlockEvent(IPhaseState phaseState) {
        return phaseState == Post.UNWINDING;
    }

    @Override
    public boolean alreadyCapturingEntitySpawns(IPhaseState state) {
        return state == Post.UNWINDING;
    }

    @Override
    public boolean alreadyCapturingEntityTicks(IPhaseState state) {
        return state == Post.UNWINDING;
    }

    @Override
    public boolean alreadyCapturingTileTicks(IPhaseState state) {
        return state == Post.UNWINDING;
    }

    @Override
    public boolean alreadyCapturingItemSpawns(IPhaseState currentState) {
        return currentState == Post.UNWINDING;
    }

    @Override
    public boolean ignoresItemPreMerging(IPhaseState currentState) {
        return currentState == State.COMMAND || currentState == State.COMPLETE || super.ignoresItemPreMerging(currentState);
    }

    @Override
    public boolean requiresBlockCapturing(IPhaseState currentState) {
        return currentState != State.COMPLETE;
    }

    @Override
    public void associateNeighborStateNotifier(IPhaseState state, PhaseContext context, @Nullable BlockPos sourcePos, Block block, BlockPos notifyPos,
            WorldServer minecraftWorld, PlayerTracker.Type notifier) {
        if (state == Post.UNWINDING) {
            final IPhaseState unwindingState = context.firstNamed(InternalNamedCauses.Tracker.UNWINDING_STATE, IPhaseState.class)
                    .orElseThrow(TrackingUtil.throwWithContext("Intended to be unwinding a phase but no phase unwinding found!", context));
            final PhaseContext unwindingContext = context.firstNamed(InternalNamedCauses.Tracker.UNWINDING_CONTEXT, PhaseContext.class)
                    .orElseThrow(TrackingUtil.throwWithContext("Intended to be unwinding a phase with a context, but no context found!", context));
            unwindingState.getPhase()
                    .associateNeighborStateNotifier(unwindingState, unwindingContext, sourcePos, block, notifyPos, minecraftWorld, notifier);
        } else if (state == State.COMMAND) {
            context.getSource(Player.class)
                    .ifPresent(player -> ((IMixinChunk) minecraftWorld.getChunkFromBlockCoords(notifyPos))
                            .setBlockNotifier(notifyPos, player.getUniqueId()));

        }
    }

    @Override
    public boolean ignoresScheduledUpdates(IPhaseState phaseState) {
        return phaseState == Post.UNWINDING;
    }

    @Override
    public boolean spawnEntityOrCapture(CauseTracker causeTracker, IPhaseState phaseState, PhaseContext context, Entity entity, int chunkX,
            int chunkZ) {
        return phaseState != State.COMPLETE
               ? context.getCapturedEntities().add(entity)
               : super.spawnEntityOrCapture(causeTracker, phaseState, context, entity, chunkX, chunkZ);
    }

    @Override
    public void appendContextPreExplosion(PhaseContext phaseContext, PhaseData currentPhaseData) {
        if (currentPhaseData.state == Post.UNWINDING) {
            ((PostState) currentPhaseData.state).appendContextPreExplosion(phaseContext, currentPhaseData);
            return;
        }
        super.appendContextPreExplosion(phaseContext, currentPhaseData);
    }
}
