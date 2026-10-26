package com.farmconnect.controller;

import com.farmconnect.dto.Calendar.CalendarResponse;
import com.farmconnect.model.User;
import com.farmconnect.service.FarmCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The farmer's calendar: jobs worked out from their planting records and the weather forecast. */
@RestController
@RequestMapping("/api/calendar")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class CalendarController {
    private final FarmCalendarService calendar;

    @GetMapping
    public CalendarResponse mine(@AuthenticationPrincipal User me) { return calendar.forFarmer(me); }
}
