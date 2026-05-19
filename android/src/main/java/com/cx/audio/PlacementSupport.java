package com.cx.audio;

import java.util.Objects;

public final class PlacementSupport {
    private final PlacementType placementType;
    private final TargetPlatform targetPlatform;
    private final PlacementSupportStatus status;

    public PlacementSupport(
            PlacementType placementType,
            TargetPlatform targetPlatform,
            PlacementSupportStatus status) {
        this.placementType = Objects.requireNonNull(placementType, "placementType");
        this.targetPlatform = Objects.requireNonNull(targetPlatform, "targetPlatform");
        this.status = Objects.requireNonNull(status, "status");
    }

    public PlacementType getPlacementType() {
        return placementType;
    }

    public TargetPlatform getTargetPlatform() {
        return targetPlatform;
    }

    public PlacementSupportStatus getStatus() {
        return status;
    }

    public boolean isSupported() {
        return status == PlacementSupportStatus.SUPPORTED;
    }
}
