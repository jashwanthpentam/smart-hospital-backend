package com.hospital.service;

import com.hospital.dto.AppointmentRequest;
import com.hospital.dto.AppointmentResponse;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ConflictException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              QueueEntryRepository queueEntryRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              DoctorScheduleRepository doctorScheduleRepository) {
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<AppointmentResponse> getAppointmentsPaged(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(this::mapToResponse);
    }

    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return mapToResponse(appointment);
    }

    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<AppointmentResponse> getAppointmentsByPatientPaged(Long patientId, Pageable pageable) {
        return appointmentRepository.findByPatientId(patientId, pageable).map(this::mapToResponse);
    }

    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<AppointmentResponse> getAppointmentsByDoctorPaged(Long doctorId, Pageable pageable) {
        return appointmentRepository.findByDoctorId(doctorId, pageable).map(this::mapToResponse);
    }

    /**
     * Appointment Booking workflow:
     * 1. Patient authenticated / verified
     * 2. Doctor exists
     * 3. Doctor schedule exists for date
     * 4. Time within doctor schedule
     * 5. Capacity available
     * 6. Patient has no conflicting active appointment
     * 7. Save Appointment (WAITING)
     * 8. Save QueueEntry (WAITING, priority score)
     */
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, Long authenticatedPatientId) {
        Long patientId = (request.getPatientId() != null) ? request.getPatientId() : authenticatedPatientId;
        if (patientId == null) {
            throw new BadRequestException("Patient ID is required");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + request.getDoctorId()));

        DoctorSchedule schedule = doctorScheduleRepository.findByDoctorIdAndAvailableDate(
                doctor.getId(), request.getAppointmentDate()
        ).orElseThrow(() -> new BadRequestException(
                "Doctor has no schedule available on " + request.getAppointmentDate()
        ));

        // 6-HOUR BOOKING CUTOFF: Booking must occur at least 6 hours before schedule start time
        LocalDateTime scheduleStartDateTime = LocalDateTime.of(schedule.getAvailableDate(), schedule.getStartTime());
        LocalDateTime bookingCutoffDateTime = scheduleStartDateTime.minusHours(6);
        if (LocalDateTime.now().isAfter(bookingCutoffDateTime)) {
            throw new BadRequestException("Booking closed. Appointments for this schedule must be booked at least 6 hours before start time (" +
                    schedule.getStartTime() + "). Booking cutoff was " + bookingCutoffDateTime.toLocalTime() + ".");
        }

        // HARD SCHEDULE CAPACITY CHECK: Cancelled appointments do not consume capacity
        long bookedCount = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(
                doctor.getId(), request.getAppointmentDate(), AppointmentStatus.CANCELLED
        );
        if (bookedCount >= schedule.getMaxAppointments()) {
            throw new ConflictException("Schedule is full. New bookings are closed.");
        }

        // ONE-BOOKING-PER-SCHEDULE: A patient may hold only ONE active appointment per doctor schedule.
        // Active statuses: WAITING, BOOKED, IN_PROGRESS. COMPLETED and CANCELLED do not block rebooking.
        boolean alreadyBookedInSchedule = appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                patientId, doctor.getId(), request.getAppointmentDate(),
                java.util.List.of(AppointmentStatus.WAITING, AppointmentStatus.BOOKED, AppointmentStatus.IN_PROGRESS)
        );
        if (alreadyBookedInSchedule) {
            throw new ConflictException("You already have an appointment in this schedule.");
        }

        // INTERNAL 15-MINUTE DURATION SLOT ALLOCATION:
        // Patients book the doctor's schedule shift without selecting individual 15-min slots.
        // We maintain appointmentTime internally to avoid collisions and prevent double bookings.
        LocalTime assignedTime;
        if (request.getAppointmentTime() != null) {
            // If explicit appointmentTime is provided (e.g. from existing test suites or admin)
            if (request.getAppointmentTime().isBefore(schedule.getStartTime()) ||
                request.getAppointmentTime().isAfter(schedule.getEndTime())) {
                throw new BadRequestException("Appointment time must be between " +
                        schedule.getStartTime() + " and " + schedule.getEndTime());
            }

            boolean isDoctorSlotBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                    doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime(), AppointmentStatus.CANCELLED
            );
            if (isDoctorSlotBooked) {
                throw new ConflictException("Doctor already has an active appointment scheduled at " +
                        request.getAppointmentDate() + " " + request.getAppointmentTime() + ". Please select another time slot.");
            }
            assignedTime = request.getAppointmentTime();
        } else {
            // Automatically allocate the first available 15-minute slot within the schedule shift
            List<Appointment> activeBookings = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(
                    doctor.getId(), request.getAppointmentDate(), AppointmentStatus.CANCELLED
            );
            Set<LocalTime> bookedSlots = activeBookings.stream()
                    .map(Appointment::getAppointmentTime)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            LocalTime slot = schedule.getStartTime();
            LocalTime foundSlot = null;
            while (!slot.isAfter(schedule.getEndTime().minusMinutes(15))) {
                if (!bookedSlots.contains(slot)) {
                    foundSlot = slot;
                    break;
                }
                slot = slot.plusMinutes(15);
            }

            if (foundSlot == null) {
                throw new ConflictException("Schedule is full. New bookings are closed.");
            }
            assignedTime = foundSlot;
        }

        // Check for conflicting appointment for the same patient at that date and time
        boolean hasConflict = appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                patientId, request.getAppointmentDate(), assignedTime, AppointmentStatus.CANCELLED
        );
        if (hasConflict) {
            throw new ConflictException("Patient already has an active appointment at " +
                    request.getAppointmentDate() + " " + assignedTime);
        }

        // Create Appointment
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(assignedTime);
        appointment.setPriorityType(request.getPriorityType());
        appointment.setSymptoms(request.getSymptoms());
        appointment.setStatus(AppointmentStatus.WAITING);
        appointment.setCreatedAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Calculate Priority Score: EMERGENCY=100, URGENT=50, NORMAL=20
        int score = request.getPriorityType() != null ? request.getPriorityType().getDefaultScore() : 20;

        // Create corresponding QueueEntry
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setAppointment(savedAppointment);
        queueEntry.setPriorityScore(score);
        queueEntry.setQueueStatus(QueueStatus.WAITING);
        queueEntry.setArrivalTime(LocalDateTime.now());

        queueEntryRepository.save(queueEntry);

        log.info("Booked appointment #{} for patient {} with Dr. {} on {} at {} [Priority: {}, Score: {}]",
                savedAppointment.getId(), patient.getUser().getName(), doctor.getUser().getName(),
                savedAppointment.getAppointmentDate(), savedAppointment.getAppointmentTime(),
                savedAppointment.getPriorityType(), score);

        return mapToResponse(savedAppointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, Long patientId, boolean isAdminOrDoctor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (!isAdminOrDoctor && !appointment.getPatient().getId().equals(patientId)) {
            throw new BadRequestException("You are not authorized to cancel this appointment");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel an appointment that is already completed");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        // Update queue entry if exists
        queueEntryRepository.findByAppointmentId(appointmentId).ifPresent(q -> {
            q.setQueueStatus(QueueStatus.SKIPPED);
            queueEntryRepository.save(q);
        });

        log.info("Cancelled appointment #{}", appointmentId);
        return mapToResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        if (request.getPriorityType() != null && request.getPriorityType() != appointment.getPriorityType()) {
            appointment.setPriorityType(request.getPriorityType());
            queueEntryRepository.findByAppointmentId(id).ifPresent(q -> {
                q.setPriorityScore(request.getPriorityType().getDefaultScore());
                queueEntryRepository.save(q);
            });
        }

        if (request.getSymptoms() != null) {
            appointment.setSymptoms(request.getSymptoms());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteAppointment(Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Appointment not found with id: " + id);
        }
        appointmentRepository.deleteById(id);
    }

    public AppointmentResponse mapToResponse(Appointment appointment) {
        QueueEntry q = queueEntryRepository.findByAppointmentId(appointment.getId()).orElse(null);

        AppointmentResponse res = new AppointmentResponse();
        res.setId(appointment.getId());
        res.setPatientId(appointment.getPatient().getId());
        res.setPatientName(appointment.getPatient().getUser().getName());
        res.setPatientPhone(appointment.getPatient().getPhone());
        res.setPatientAge(appointment.getPatient().getAge());
        res.setDoctorId(appointment.getDoctor().getId());
        res.setDoctorName(appointment.getDoctor().getUser().getName());
        res.setDepartmentName(appointment.getDoctor().getDepartment().getName());
        res.setAppointmentDate(appointment.getAppointmentDate());
        res.setAppointmentTime(appointment.getAppointmentTime());
        res.setPriorityType(appointment.getPriorityType());
        res.setSymptoms(appointment.getSymptoms());
        res.setStatus(appointment.getStatus());
        res.setCreatedAt(appointment.getCreatedAt());

        if (q != null) {
            res.setPriorityScore(q.getPriorityScore());
            res.setQueueStatus(q.getQueueStatus());
            res.setArrivalTime(q.getArrivalTime());
        }

        return res;
    }
}
