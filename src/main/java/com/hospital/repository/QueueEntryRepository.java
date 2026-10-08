package com.hospital.repository;

import com.hospital.entity.Appointment;
import com.hospital.entity.QueueEntry;
import com.hospital.entity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    Optional<QueueEntry> findByAppointment(Appointment appointment);
    Optional<QueueEntry> findByAppointmentId(Long appointmentId);

    // Find all entries for a specific doctor and date with WAITING status
    @Query("SELECT q FROM QueueEntry q WHERE q.appointment.doctor.id = :doctorId AND q.appointment.appointmentDate = :date AND q.queueStatus = :status")
    List<QueueEntry> findByDoctorIdAndDateAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("status") QueueStatus status
    );

    // Find active entry (CALLED or IN_PROGRESS) for a doctor on a date
    @Query("SELECT q FROM QueueEntry q WHERE q.appointment.doctor.id = :doctorId AND q.appointment.appointmentDate = :date AND q.queueStatus IN (:statuses)")
    List<QueueEntry> findActiveByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("statuses") List<QueueStatus> statuses
    );

    long countByQueueStatus(QueueStatus queueStatus);
}
