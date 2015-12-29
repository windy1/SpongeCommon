package org.spongepowered.common.event.tracking.phase.plugin;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;

public class FakeplayerState extends PluginPhaseState {

    @Override
    void processPostTick(CauseTracker causeTracker, PhaseContext phaseContext) {
        phaseContext.getCapturedBlockSupplier()
                .ifPresentAndNotEmpty(list -> TrackingUtil.processBlockCaptures(list, causeTracker, this, phaseContext));
    }

    public void associateAdditionalBlockChangeCauses(PhaseContext context, Cause.Builder builder, CauseTracker causeTracker) {
        builder.named(NamedCause.PLAYER_SIMULATED, context.firstNamed(NamedCause.PLAYER_SIMULATED, Object.class)
                .orElseThrow(TrackingUtil.throwWithContext("Player simulator not found in cause", context)));
    }
}
