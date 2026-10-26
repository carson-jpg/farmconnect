package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CropDoctorGuideTest {
    @Test void everyProblemUsesOnlyKnownSymptomsAndCrops() {
        List<String> crops = CropDoctorGuide.crops();
        for (CropDoctorGuide.Problem p : CropDoctorGuide.problems()) {
            for (String k : p.symptoms().keySet()) assertTrue(CropDoctorGuide.knownSymptom(k), p.key() + " uses unknown symptom " + k);
            for (String c : p.crops()) assertTrue(crops.contains(c), p.key() + " uses unknown crop " + c);
            assertFalse(p.advice().isEmpty(), p.key() + " has no advice");
        }
    }

    @Test void problemKeysAreUnique() {
        long distinct = CropDoctorGuide.problems().stream().map(CropDoctorGuide.Problem::key).distinct().count();
        assertEquals(CropDoctorGuide.problems().size(), distinct);
    }

    @Test void fallArmywormIsTheTopGuessForChewedMaizeWithDroppings() {
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose("maize", List.of("HOLES_IN_LEAVES", "FRASS"));
        assertFalse(m.isEmpty());
        assertEquals("FALL_ARMYWORM", m.get(0).key());
        assertEquals("HIGH", m.get(0).confidence());
        assertTrue(m.get(0).seeOfficer());
    }

    @Test void suddenWiltingInTomatoPointsToBacterialWilt() {
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose("Tomatoes", List.of("WILTING"));
        assertEquals("BACTERIAL_WILT", m.get(0).key());
    }

    @Test void problemsForOtherCropsAreNotSuggested() {
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose("Beans", List.of("FRASS", "HOLES_IN_LEAVES"));
        for (CropDoctorGuide.Match x : m) assertNotEquals("FALL_ARMYWORM", x.key());
    }

    @Test void unknownCropStillGetsGeneralProblemsOnly() {
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose("Pumpkin", List.of("PURPLE_LEAVES"));
        assertEquals(1, m.size());
        assertEquals("PHOSPHORUS_HUNGER", m.get(0).key());
    }

    @Test void nothingTickedOrUnknownSymptomsGiveNoGuess() {
        assertTrue(CropDoctorGuide.diagnose("Maize", List.of()).isEmpty());
        assertTrue(CropDoctorGuide.diagnose("Maize", List.of("NOT_A_SYMPTOM")).isEmpty());
        assertTrue(CropDoctorGuide.diagnose("Maize", null).isEmpty());
    }

    @Test void atMostThreeGuessesBestFirst() {
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose("Maize", List.of("YELLOW_LEAVES", "STUNTED", "LEAF_SPOTS", "HOLES_IN_LEAVES", "BROWN_EDGES"));
        assertTrue(m.size() <= 3);
        for (int i = 1; i < m.size(); i++) assertTrue(m.get(i - 1).score() >= m.get(i).score());
    }

    @Test void weakMatchesAlwaysSuggestSeeingTheOfficer() {
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose("Maize", List.of("YELLOW_LEAVES", "WILTING", "PURPLE_LEAVES", "WEBBING", "FRUIT_ROT"));
        for (CropDoctorGuide.Match x : m) if ("LOW".equals(x.confidence())) assertTrue(x.seeOfficer());
    }
}
