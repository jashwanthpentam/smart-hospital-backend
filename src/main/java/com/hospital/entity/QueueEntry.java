package com.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 20)
    private QueueStatus queueStatus;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    public QueueEntry() {
    }

    public QueueEntry(Long id, Appointment appointment, Integer priorityScore, QueueStatus queueStatus, LocalDateTime arrivalTime) {
        this.id = id;
        this.appointment = appointment;
        this.priorityScore = priorityScore;
        this.queueStatus = queueStatus;
        this.arrivalTime = arrivalTime;
    }

    @PrePersist
    protected void onCreate() {
        if (this.arrivalTime == null) {
            this.arrivalTime = LocalDateTime.now();
        }
        if (this.queueStatus == null) {
            this.queueStatus = QueueStatus.WAITING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(QueueStatus queueStatus) {
        this.queueStatus = queueStatus;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
}
