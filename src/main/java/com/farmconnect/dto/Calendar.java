package com.farmconnect.dto;

import java.util.List;

public final class Calendar {
    private Calendar() {}

    /** One job on a farmer's calendar. status: OVERDUE, DUE_NOW or UPCOMING. */
    public record CalendarItem(Long recordId, String crop, String icon, String kind, String title, String advice,
                               String planted, String from, String to, String status, String weatherNote, boolean goodToGo) {}
    public record CalendarResponse(String place, String region, List<CalendarItem> items, List<String> plantNow,
                                   String plantNowNote, boolean weatherKnown, String disclaimer) {}
}
