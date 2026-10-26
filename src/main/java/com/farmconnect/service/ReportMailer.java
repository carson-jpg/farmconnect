package com.farmconnect.service;

import com.farmconnect.controller.AnalyticsController;
import com.farmconnect.dto.Community.Analytics;
import com.farmconnect.dto.Community.Indicators;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.report.ReportPdf;
import com.farmconnect.report.ReportXlsx;
import com.farmconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emails the county report (PDF + Excel) every Monday 07:00 to each super admin (whole country), county admin and
 * assigned officer (their own area). Officers without an area are skipped so nobody receives data outside their area.
 */
@Service
@RequiredArgsConstructor
public class ReportMailer {
    private static final Logger log = LoggerFactory.getLogger(ReportMailer.class);
    private final AnalyticsController analytics;
    private final UserRepository users;
    private final MailService mail;
    @Value("${app.reports.weekly.enabled:true}") private boolean enabled;

    @Scheduled(cron = "${app.reports.weekly.cron:0 0 7 * * MON}", zone = "Africa/Nairobi")
    @Transactional(readOnly = true)
    public void sendWeekly() {
        if (!enabled) return;
        Map<String, Analytics> cache = new HashMap<>();      // one calculation per area, however many people get it
        int sent = 0;
        for (User u : users.findAll()) {
            if (!u.isEnabled() || u.getEmail() == null || u.getEmail().isBlank()) continue;
            if (u.getRole() != Role.ADMIN && u.getRole() != Role.COUNTY_ADMIN && u.getRole() != Role.OFFICER) continue;
            AreaScope sc = AreaScope.of(u);
            if (sc.none() || (u.getRole() == Role.OFFICER && sc.all())) continue;
            try {
                Analytics a = cache.computeIfAbsent(sc.label(), k -> analytics.compute(sc));
                send(u, a, "Your weekly FarmConnect report: " + a.scopeLabel());
                sent++;
            } catch (Exception e) {
                log.warn("Weekly report to user {} failed: {}", u.getId(), e.getMessage());
            }
        }
        log.info("Weekly reports sent: {}", sent);
    }

    /** Sends the current report for this person's area to their own email right now. */
    @Transactional(readOnly = true)
    public void sendNow(User u) {
        AreaScope sc = AreaScope.of(u);
        if (sc.none()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "You have no county assigned yet");
        Analytics a = analytics.compute(sc);
        send(u, a, "Your FarmConnect report: " + a.scopeLabel());
    }

    private void send(User u, Analytics a, String subject) {
        Indicators i = a.indicators();
        String date = LocalDate.now(ZoneId.of("Africa/Nairobi")).toString();
        String text = "Hello " + (u.getName() == null ? "" : u.getName()) + ",\n\n"
                + "Here is the FarmConnect report for " + a.scopeLabel() + " (" + date + ").\n\n"
                + "Registered farmers: " + i.registeredFarmers() + " (" + i.verifiedFarmers() + " verified)\n"
                + "Registered buyers: " + i.registeredBuyers() + "\n"
                + "Active users in the last 30 days: " + i.activeUsers30d() + "\n"
                + "Farmer groups: " + i.groupsOnboarded() + " with " + i.groupMembers() + " members\n"
                + "Market listings: " + i.marketListings() + "\n\n"
                + "The PDF and Excel files are attached.\n\nFarmConnect";
        byte[] pdf = ReportPdf.build(a, u.getName());
        byte[] xlsx = ReportXlsx.build(a, u.getName());
        mail.sendWithAttachments(u.getEmail(), subject, text, List.of(
                new MailService.Attachment("farmconnect-report-" + date + ".pdf", "application/pdf", pdf),
                new MailService.Attachment("farmconnect-report-" + date + ".xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx)));
    }
}
