package com.farmconnect.repository;

import com.farmconnect.model.CalendarReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarReminderRepository extends JpaRepository<CalendarReminder, Long> {
    boolean existsByRecordIdAndTaskKey(Long recordId, String taskKey);
}
