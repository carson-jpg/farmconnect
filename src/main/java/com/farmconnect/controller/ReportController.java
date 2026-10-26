package com.farmconnect.controller;

import com.farmconnect.model.User;
import com.farmconnect.report.ReportXlsx;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.ReportMailer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Excel download and "email it to me now" for the area report (the PDF is at /api/analytics/report.pdf). */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN','OFFICER')")
@RequiredArgsConstructor
public class ReportController {
    private final AnalyticsController analytics;
    private final ReportMailer mailer;

    @GetMapping("/county.xlsx")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> xlsx(@AuthenticationPrincipal User me) {
        byte[] bytes = ReportXlsx.build(analytics.compute(AreaScope.of(me)), me.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=farmconnect-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(bytes);
    }

    @PostMapping("/email-me")
    public Map<String, String> emailMe(@AuthenticationPrincipal User me) {
        mailer.sendNow(me);
        return Map.of("message", "The report was sent to " + me.getEmail());
    }
}
