package com.farmconnect.service;

import com.farmconnect.dto.Calendar.CalendarItem;
import com.farmconnect.model.Farm;
import com.farmconnect.model.FarmRecord;
import com.farmconnect.model.RecordType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FarmCalendarTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 20);
    private final FarmCalendarService svc = new FarmCalendarService(null, null, null, null, null, null, null);

    private static FarmRecord record(RecordType type, String title, LocalDate date, long farmId) {
        Farm f = new Farm();
        f.setId(farmId);
        FarmRecord r = new FarmRecord();
        r.setId(farmId * 100 + date.getDayOfYear());
        r.setFarm(f);
        r.setType(type);
        r.setTitle(title);
        r.setRecordDate(date);
        return r;
    }

    @Test void findsCropsByEverydayNames() {
        assertEquals("Maize", CropCalendar.find("Maize (hybrid)").name());
        assertEquals("Kale", CropCalendar.find("sukuma wiki").name());
        assertEquals("Irish Potato", CropCalendar.find("Irish potatoes").name());
        assertNull(CropCalendar.find("Sweet potato"));
        assertNull(CropCalendar.find("Dairy cows"));
    }

    @Test void maizePlanted20DaysAgoHasFirstWeedingDueNow() {
        List<FarmRecord> mine = List.of(record(RecordType.PLANTING, "Maize", TODAY.minusDays(20), 1));
        List<CalendarItem> items = svc.items(mine, WeatherAdvice.Outlook.unknown(), TODAY);
        CalendarItem weeding = items.stream().filter(i -> i.title().equals("First weeding")).findFirst().orElseThrow();
        assertEquals("DUE_NOW", weeding.status());
        assertTrue(items.stream().noneMatch(i -> i.kind().equals("HARVEST")), "harvest is months away");
    }

    @Test void harvestedFieldStopsShowingJobs() {
        List<FarmRecord> mine = List.of(
                record(RecordType.PLANTING, "Maize", TODAY.minusDays(150), 1),
                record(RecordType.HARVEST, "Maize", TODAY.minusDays(2), 1));
        assertTrue(svc.items(mine, WeatherAdvice.Outlook.unknown(), TODAY).isEmpty());
    }

    @Test void harvestOnAnotherFarmDoesNotHideThisFarmsJobs() {
        List<FarmRecord> mine = List.of(
                record(RecordType.PLANTING, "Maize", TODAY.minusDays(150), 1),
                record(RecordType.HARVEST, "Maize", TODAY.minusDays(2), 2));
        assertFalse(svc.items(mine, WeatherAdvice.Outlook.unknown(), TODAY).isEmpty());
    }

    @Test void rainAfterSprayingJobSaysWait() {
        WeatherAdvice.Outlook wet = new WeatherAdvice.Outlook(true, 12, 20, 40, 80, 10, 70);
        WeatherAdvice.Note n = WeatherAdvice.forJob(CropCalendar.Kind.SPRAY, false, wet);
        assertFalse(n.goodToGo());
    }

    @Test void blightRiskSaysSprayBeforeRain() {
        WeatherAdvice.Outlook wet = new WeatherAdvice.Outlook(true, 12, 20, 40, 80, 10, 90);
        WeatherAdvice.Note n = WeatherAdvice.forJob(CropCalendar.Kind.SPRAY, true, wet);
        assertTrue(n.goodToGo());
        assertTrue(n.text().contains("BEFORE"));
    }

    @Test void plantingWindowsFollowTheRegion() {
        assertTrue(CropCalendar.plantNow(CropCalendar.Region.RIFT_HIGHLANDS, 4).contains("Maize"));
        assertFalse(CropCalendar.plantNow(CropCalendar.Region.RIFT_HIGHLANDS, 12).contains("Maize"));
        assertEquals(CropCalendar.Region.WESTERN, CropCalendar.regionOf("Bungoma"));
    }
}
