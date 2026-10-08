package com.hospital.repository;

import com.hospital.entity.Appointment;
import com.hospital.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatus(Long doctorId, LocalDate appointmentDate, AppointmentStatus status);
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNot(Long doctorId, LocalDate appointmentDate, AppointmentStatus status);
    
    // Check conflicts: same patient, same date and time, not cancelled
    boolean existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long patientId, LocalDate appointmentDate, LocalTime appointmentTime, AppointmentStatus status
    );

    // UPDATE 2: Check conflicts: same doctor, same date and time, not cancelled (prevent double booking)
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime, AppointmentStatus status
    );

    // ONE-BOOKING-PER-SCHEDULE: Check if patient already has an active appointment
    // for the same doctor+date (which uniquely maps to one DoctorSchedule)
    boolean existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
            Long patientId, Long doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> activeStatuses
    );

    // Count appointments on a date for capacity check
    long countByDoctorIdAndAppointmentDateAndStatusNot(Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    Page<Appointment> findAll(Pageable pageable);
    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    long countByStatus(AppointmentStatus status);
}
