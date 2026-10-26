package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Remembers which calendar reminders were already sent, so a farmer hears about each job once. */
@Entity
@Table(name = "calendar_reminders", uniqueConstraints = @UniqueConstraint(columnNames = {"record_id", "task_key"}))
@Getter @Setter @NoArgsConstructor
public class CalendarReminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "record_id", nullable = false)
    private Long recordId;
    @Column(name = "task_key", nullable = false)
    private String taskKey;
    private Long userId;
    private LocalDate sentOn;
}
