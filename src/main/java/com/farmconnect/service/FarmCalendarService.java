package com.farmconnect.service;

import com.farmconnect.controller.WeatherController;
import com.farmconnect.dto.Calendar.*;
import com.farmconnect.dto.Community.WeatherDay;
import com.farmconnect.dto.Community.WeatherResponse;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.CropCalendar.Crop;
import com.farmconnect.service.CropCalendar.Kind;
import com.farmconnect.service.CropCalendar.Task;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** A farmer's calendar: jobs worked out from their planting records, with advice from the weather forecast. */
@Service
@RequiredArgsConstructor
public class FarmCalendarService {
    private static final Logger log = LoggerFactory.getLogger(FarmCalendarService.class);
    static final String DISCLAIMER = "This is a general guide. Your county agricultural officer's advice for this season comes first.";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.US);

    private final FarmRecordRepository records;
    private final FarmRepository farms;
    private final FarmerVerificationRepository verifications;
    private final UserRepository users;
    private final CalendarReminderRepository reminders;
    private final NotificationService notifier;
    private final WeatherController weather;

    private static LocalDate today() { return LocalDate.now(ZoneId.of("Africa/Nairobi")); }

    @Transactional(readOnly = true)
    public CalendarResponse forFarmer(User me) {
        String county = countyOf(me);
        WeatherResponse w = weatherFor(me);
        List<FarmRecord> mine = records.findByOwnerIdOrderByRecordDateDescIdDesc(me.getId());
        return build(me, county, w, mine, today());
    }

    CalendarResponse build(User me, String county, WeatherResponse w, List<FarmRecord> mine, LocalDate today) {
        WeatherAdvice.Outlook outlook = outlook(w);
        CropCalendar.Region region = CropCalendar.regionOf(county);
        List<CalendarItem> items = items(mine, outlook, today);
        List<String> plantNow = CropCalendar.plantNow(region, today.getMonthValue());
        return new CalendarResponse(w == null ? (county == null ? "Your area" : county + " County") : w.place(),
                regionLabel(region), items, plantNow, WeatherAdvice.planting(outlook), outlook.known(), DISCLAIMER);
    }

    /** The jobs due now or soon, worked out from planting records. */
    List<CalendarItem> items(List<FarmRecord> mine, WeatherAdvice.Outlook outlook, LocalDate today) {
        List<CalendarItem> out = new ArrayList<>();
        for (FarmRecord p : mine) {
            if (p.getType() != RecordType.PLANTING || p.getRecordDate() == null) continue;
            Crop crop = CropCalendar.find(p.getTitle());
            if (crop == null) continue;
            long age = ChronoUnit.DAYS.between(p.getRecordDate(), today);
            if (age < 0 || age > 220) continue;
            boolean harvested = mine.stream().anyMatch(r -> r.getType() == RecordType.HARVEST && r.getRecordDate() != null
                    && !r.getRecordDate().isBefore(p.getRecordDate()) && crop == CropCalendar.find(r.getTitle())
                    && r.getFarm() != null && p.getFarm() != null && r.getFarm().getId().equals(p.getFarm().getId()));
            if (harvested) continue;
            boolean blightCrop = crop.name().equals("Irish Potato") || crop.name().equals("Tomato");
            for (Task t : crop.tasks()) {
                LocalDate from = p.getRecordDate().plusDays(t.fromDay()), to = p.getRecordDate().plusDays(t.toDay());
                String status;
                if (today.isBefore(from)) {
                    long until = ChronoUnit.DAYS.between(today, from);
                    if (until > (t.kind() == Kind.HARVEST ? 21 : 14)) continue;
                    status = "UPCOMING";
                } else if (!today.isAfter(to)) status = "DUE_NOW";
                else if (ChronoUnit.DAYS.between(to, today) <= 7) status = "OVERDUE";
                else continue;
                boolean soon = !status.equals("UPCOMING") || ChronoUnit.DAYS.between(today, from) <= 3;
                WeatherAdvice.Note note = soon ? WeatherAdvice.forJob(t.kind(), blightCrop && t.kind() == Kind.SPRAY, outlook) : new WeatherAdvice.Note(null, true);
                out.add(new CalendarItem(p.getId(), p.getTitle(), crop.icon(), t.kind().name(), t.title(), t.advice(),
                        p.getRecordDate().format(DAY), from.format(DAY), to.format(DAY), status, note.text(), note.goodToGo()));
            }
        }
        out.sort(Comparator.comparing((CalendarItem i) -> rank(i.status())).thenComparing(CalendarItem::from));
        return out;
    }

    private static int rank(String s) { return switch (s) { case "OVERDUE" -> 0; case "DUE_NOW" -> 1; default -> 2; }; }

    static WeatherAdvice.Outlook outlook(WeatherResponse w) {
        if (w == null || w.days() == null || w.days().isEmpty()) return WeatherAdvice.Outlook.unknown();
        List<WeatherDay> d = w.days();
        double r2 = d.stream().limit(2).mapToDouble(WeatherDay::rainMm).sum();
        double r3 = d.stream().limit(3).mapToDouble(WeatherDay::rainMm).sum();
        double r7 = d.stream().mapToDouble(WeatherDay::rainMm).sum();
        int prob = d.stream().limit(2).mapToInt(WeatherDay::rainProb).max().orElse(0);
        return new WeatherAdvice.Outlook(true, r2, r3, r7, prob, w.windKmh(), w.humidity());
    }

    private WeatherResponse weatherFor(User me) {
        try { return weather.weather(null, null, null, me); } catch (Exception e) { return null; }   // no forecast: the calendar still works
    }

    private String countyOf(User me) {
        for (Farm f : farms.findByOwnerId(me.getId())) {
            String c = Locations.countyOf(f.getSubCounty());
            if (c != null) return c;
        }
        return verifications.findByFarmerId(me.getId()).map(v -> Locations.countyOf(v.getSubCounty())).orElse(null);
    }

    private static String regionLabel(CropCalendar.Region r) {
        return switch (r) { case WESTERN -> "Western Kenya"; case EASTERN_CENTRAL -> "Eastern and Central Kenya"; default -> "Rift Valley highlands"; };
    }

    // ------------------------------------------------------------------ daily reminders

    /** Every morning at 06:30 tell each farmer about jobs that have just become due (once per job). */
    @Scheduled(cron = "${calendar.reminders.cron:0 30 6 * * *}", zone = "Africa/Nairobi")
    @Transactional
    public void sendDailyReminders() {
        try {
            LocalDate today = today();
            List<FarmRecord> recent = records.findByTypeAndRecordDateGreaterThanEqual(RecordType.PLANTING, today.minusDays(220));
            Set<Long> ownerIds = recent.stream().map(r -> r.getOwner().getId()).collect(Collectors.toSet());
            if (ownerIds.isEmpty()) return;
            Map<Long, User> owners = users.findAllById(ownerIds).stream().filter(User::isEnabled).collect(Collectors.toMap(User::getId, u -> u));
            int sent = 0;
            for (User u : owners.values()) {
                List<FarmRecord> mine = records.findByOwnerIdOrderByRecordDateDescIdDesc(u.getId());
                WeatherAdvice.Outlook outlook = outlook(weatherFor(u));
                for (CalendarItem i : items(mine, outlook, today)) {
                    if (!i.status().equals("DUE_NOW")) continue;
                    String key = taskKey(i);
                    if (reminders.existsByRecordIdAndTaskKey(i.recordId(), key)) continue;
                    CalendarReminder r = new CalendarReminder();
                    r.setRecordId(i.recordId()); r.setTaskKey(key); r.setUserId(u.getId()); r.setSentOn(today);
                    reminders.save(r);
                    String body = i.title() + " for your " + i.crop() + " (planted " + i.planted() + ", due " + i.from() + " to " + i.to() + "). "
                            + (i.weatherNote() == null ? i.advice() : i.weatherNote());
                    notifier.notifyAndSms(u, "CALENDAR", i.icon() + " Farm job due: " + i.title(), body, i.recordId());
                    sent++;
                }
            }
            log.info("Calendar reminders sent: {}", sent);
        } catch (Exception e) {
            log.warn("Calendar reminders failed: {}", e.getMessage());
        }
    }

    private static String taskKey(CalendarItem i) {
        // title is unique per crop task, so it is a stable key together with the record
        return i.kind() + ":" + i.title();
    }
}
