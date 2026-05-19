package com.cx.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PlacementMatrixTest {
    @Test
    public void androidSupportsAllThreePlacementTypes() {
        assertSupported(PlacementType.BACKGROUND_AUDIO);
        assertSupported(PlacementType.STICKY_PLAYER);
        assertSupported(PlacementType.FULLSCREEN_INTERSTITIAL);
    }

    @Test
    public void allIncludesEveryPlacementAndTargetCombination() {
        int expectedCount = PlacementType.values().length * TargetPlatform.values().length;

        assertEquals(expectedCount, PlacementMatrix.all().size());
    }

    @Test
    public void nonAndroidPlacementsRemainPlanned() {
        for (PlacementType placementType : PlacementType.values()) {
            assertEquals(
                    PlacementSupportStatus.PLANNED,
                    PlacementMatrix.supportFor(placementType, TargetPlatform.IOS).getStatus());
            assertEquals(
                    PlacementSupportStatus.PLANNED,
                    PlacementMatrix.supportFor(placementType, TargetPlatform.REACT_NATIVE).getStatus());
            assertEquals(
                    PlacementSupportStatus.PLANNED,
                    PlacementMatrix.supportFor(placementType, TargetPlatform.UNITY).getStatus());
        }
    }

    private static void assertSupported(PlacementType placementType) {
        PlacementSupport support = PlacementMatrix.supportFor(placementType, TargetPlatform.ANDROID);

        assertEquals(PlacementSupportStatus.SUPPORTED, support.getStatus());
        assertTrue(support.isSupported());
    }
}
