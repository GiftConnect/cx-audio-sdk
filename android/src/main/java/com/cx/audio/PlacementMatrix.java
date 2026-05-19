package com.cx.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PlacementMatrix {
    private static final Map<PlacementType, Map<TargetPlatform, PlacementSupportStatus>> MATRIX =
            buildMatrix();

    private PlacementMatrix() {
    }

    public static PlacementSupport supportFor(
            PlacementType placementType,
            TargetPlatform targetPlatform) {
        Objects.requireNonNull(placementType, "placementType");
        Objects.requireNonNull(targetPlatform, "targetPlatform");

        PlacementSupportStatus status = MATRIX.get(placementType).get(targetPlatform);
        return new PlacementSupport(placementType, targetPlatform, status);
    }

    public static List<PlacementSupport> all() {
        List<PlacementSupport> support = new ArrayList<>();
        for (PlacementType placementType : PlacementType.values()) {
            for (TargetPlatform targetPlatform : TargetPlatform.values()) {
                support.add(supportFor(placementType, targetPlatform));
            }
        }
        return Collections.unmodifiableList(support);
    }

    private static Map<PlacementType, Map<TargetPlatform, PlacementSupportStatus>> buildMatrix() {
        Map<PlacementType, Map<TargetPlatform, PlacementSupportStatus>> mutableMatrix =
                new EnumMap<>(PlacementType.class);
        for (PlacementType placementType : PlacementType.values()) {
            Map<TargetPlatform, PlacementSupportStatus> platforms =
                    new EnumMap<>(TargetPlatform.class);
            for (TargetPlatform targetPlatform : TargetPlatform.values()) {
                platforms.put(targetPlatform, PlacementSupportStatus.PLANNED);
            }
            mutableMatrix.put(placementType, platforms);
        }

        mutableMatrix.get(PlacementType.STICKY_PLAYER)
                .put(TargetPlatform.ANDROID, PlacementSupportStatus.SUPPORTED);
        mutableMatrix.get(PlacementType.BACKGROUND_AUDIO)
                .put(TargetPlatform.ANDROID, PlacementSupportStatus.SUPPORTED);
        mutableMatrix.get(PlacementType.FULLSCREEN_INTERSTITIAL)
                .put(TargetPlatform.ANDROID, PlacementSupportStatus.SUPPORTED);

        Map<PlacementType, Map<TargetPlatform, PlacementSupportStatus>> matrix =
                new EnumMap<>(PlacementType.class);
        for (Map.Entry<PlacementType, Map<TargetPlatform, PlacementSupportStatus>> entry
                : mutableMatrix.entrySet()) {
            matrix.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(matrix);
    }
}
