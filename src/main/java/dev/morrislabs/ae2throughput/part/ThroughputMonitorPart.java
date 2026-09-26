package dev.morrislabs.ae2throughput.part;

import appeng.api.networking.GridFlags;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.parts.AEBasePart;

/**
 * Cable-face part that tracks every item flowing through the attached AE2 network.
 * Phase 1: skeleton that attaches to a cable and joins the grid.
 */
public class ThroughputMonitorPart extends AEBasePart {

    public ThroughputMonitorPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(1.0 / 2.0);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 13, 12, 12, 14);
    }
}
