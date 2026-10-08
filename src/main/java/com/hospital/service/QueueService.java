package com.hospital.service;

import com.hospital.dto.PatientQueueStatusResponse;
import com.hospital.dto.QueueItemResponse;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.DoctorScheduleRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.QueueEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class QueueService {

    private static final Logger log = LoggerFactory.getLogger(QueueService.class);

    /**
     * Mandatory Priority Comparator:
     * 1. Higher priorityScore first (Descending)
     * 2. Earlier arrivalTime first (Ascending) for tie-breaker
     * 3. QueueEntry ID ascending for deterministic tie-breaker
     */
    public static final Comparator<QueueEntry> PRIORITY_COMPARATOR = Comparator
            .comparingInt(QueueEntry::getPriorityScore).reversed()
            .thenComparing(QueueEntry::getArrivalTime)
            .thenComparing(QueueEntry::getId, Comparator.nullsLast(Long::compareTo));

    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public QueueService(QueueEntryRepository queueEntryRepository,
                        AppointmentRepository appointmentRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository,
                        DoctorScheduleRepository doctorScheduleRepository) {
        this.queueEntryRepository = queueEntryRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    /**
     * Builds and uses Java PriorityQueue<QueueEntry> to order all active waiting patients.
     * Excludes CANCELLED, COMPLETED, SKIPPED patients.
     */
    public List<QueueItemResponse> getDoctorQueue(Long doctorId, LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<QueueEntry> waitingEntries = queueEntryRepository.findByDoctorIdAndDateAndStatus(
                doctorId, queryDate, QueueStatus.WAITING
        );

        // Filter out any appointments that were cancelled
        List<QueueEntry> activeWaiting = waitingEntries.stream()
                .filter(q -> q.getAppointment().getStatus() != AppointmentStatus.CANCELLED
                          && q.getAppointment().getStatus() != AppointmentStatus.COMPLETED)
                .toList();

        // Use Java PriorityQueue to determine ordering
        PriorityQueue<QueueEntry> priorityQueue = new PriorityQueue<>(PRIORITY_COMPARATOR);
        priorityQueue.addAll(activeWaiting);

        List<QueueItemResponse> result = new ArrayList<>();
        int position = 1;
        while (!priorityQueue.isEmpty()) {
            QueueEntry entry = priorityQueue.poll();
            result.add(mapToQueueItemResponse(entry, position++));
        }

        return result;
    }

    /**
     * Peek the next highest priority patient from Java PriorityQueue without changing state.
     */
    public QueueItemResponse peekNextPatient(Long doctorId, LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<QueueEntry> waitingEntries = queueEntryRepository.findByDoctorIdAndDateAndStatus(
                doctorId, queryDate, QueueStatus.WAITING
        );

        List<QueueEntry> activeWaiting = waitingEntries.stream()
                .filter(q -> q.getAppointment().getStatus() != AppointmentStatus.CANCELLED
                          && q.getAppointment().getStatus() != AppointmentStatus.COMPLETED)
                .toList();

        if (activeWaiting.isEmpty()) {
            return null;
        }

        PriorityQueue<QueueEntry> priorityQueue = new PriorityQueue<>(PRIORITY_COMPARATOR);
        priorityQueue.addAll(activeWaiting);

        QueueEntry next = priorityQueue.peek();
        return next != null ? mapToQueueItemResponse(next, 1) : null;
    }

    /**
     * Finds active patient currently CALLED or IN_PROGRESS for a doctor.
     */
    public QueueItemResponse getActiveDoctorPatient(Long doctorId, LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<QueueEntry> activeList = queueEntryRepository.findActiveByDoctorIdAndDate(
                doctorId, queryDate, List.of(QueueStatus.CALLED, QueueStatus.IN_PROGRESS)
        );

        if (activeList.isEmpty()) {
            return null;
        }

        // Return the first active entry
        QueueEntry active = activeList.getFirst();
        return mapToQueueItemResponse(active, 0);
    }

    /**
     * CALL NEXT:
     * 1. Checks that no consultation is already IN_PROGRESS for this doctor.
     * 2. Finds WAITING queue entries.
     * 3. Builds Java PriorityQueue<QueueEntry>.
     * 4. Polls the highest priority patient.
     * 5. Updates queue state -> CALLED.
     * 6. Updates appointment state -> WAITING (or stays WAITING until START).
     */
    @Transactional
    public QueueItemResponse callNextPatient(Long doctorId, LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        // Rule: Do not allow doctor to call another patient while one is already IN_PROGRESS
        List<QueueEntry> inProgressList = queueEntryRepository.findActiveByDoctorIdAndDate(
                doctorId, queryDate, List.of(QueueStatus.IN_PROGRESS)
        );
        if (!inProgressList.isEmpty()) {
            throw new BadRequestException("Cannot call next patient while a consultation is currently IN_PROGRESS");
        }

        // If there is already a CALLED patient that hasn't started, check policy
        List<QueueEntry> calledList = queueEntryRepository.findActiveByDoctorIdAndDate(
                doctorId, queryDate, List.of(QueueStatus.CALLED)
        );
        if (!calledList.isEmpty()) {
            // Patient already called, return current called patient
            log.info("Doctor {} already called patient {}, returning current called patient", doctorId, calledList.getFirst().getAppointment().getPatient().getUser().getName());
            return mapToQueueItemResponse(calledList.getFirst(), 0);
        }

        List<QueueEntry> waitingEntries = queueEntryRepository.findByDoctorIdAndDateAndStatus(
                doctorId, queryDate, QueueStatus.WAITING
        );

        List<QueueEntry> activeWaiting = waitingEntries.stream()
                .filter(q -> q.getAppointment().getStatus() != AppointmentStatus.CANCELLED
                          && q.getAppointment().getStatus() != AppointmentStatus.COMPLETED)
                .toList();

        if (activeWaiting.isEmpty()) {
            throw new BadRequestException("No waiting patients in the queue");
        }

        PriorityQueue<QueueEntry> priorityQueue = new PriorityQueue<>(PRIORITY_COMPARATOR);
        priorityQueue.addAll(activeWaiting);

        QueueEntry highestPriorityEntry = priorityQueue.poll();
        if (highestPriorityEntry == null) {
            throw new BadRequestException("No patient found to call");
        }

        highestPriorityEntry.setQueueStatus(QueueStatus.CALLED);
        queueEntryRepository.save(highestPriorityEntry);

        Appointment appt = highestPriorityEntry.getAppointment();
        // Appointment remains in active state (WAITING/CALLED)
        appointmentRepository.save(appt);

        log.info("Doctor {} CALLED patient: {} with priority {} (score: {})",
                doctorId, appt.getPatient().getUser().getName(), appt.getPriorityType(), highestPriorityEntry.getPriorityScore());

        return mapToQueueItemResponse(highestPriorityEntry, 0);
    }

    /**
     * START Consultation:
     * Sets Appointment -> IN_PROGRESS and QueueEntry -> IN_PROGRESS
     */
    @Transactional
    public QueueItemResponse startConsultation(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new BadRequestException("You are not authorized to start another doctor's appointment");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot start an appointment with status: " + appointment.getStatus());
        }

        QueueEntry queueEntry = queueEntryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found for appointment: " + appointmentId));

        // UPDATE 3: A doctor must NOT be able to start directly from WAITING. Patient must first be CALLED.
        if (queueEntry.getQueueStatus() != QueueStatus.CALLED) {
            throw new BadRequestException("Patient must be CALLED before starting consultation. Current status: " + queueEntry.getQueueStatus());
        }

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        queueEntry.setQueueStatus(QueueStatus.IN_PROGRESS);

        appointmentRepository.save(appointment);
        queueEntryRepository.save(queueEntry);

        log.info("Started consultation for appointment {} with patient {}",
                appointmentId, appointment.getPatient().getUser().getName());

        return mapToQueueItemResponse(queueEntry, 0);
    }

    /**
     * COMPLETE Consultation:
     * Sets Appointment -> COMPLETED and QueueEntry -> COMPLETED
     */
    @Transactional
    public QueueItemResponse completeConsultation(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new BadRequestException("You are not authorized to complete another doctor's appointment");
        }

        QueueEntry queueEntry = queueEntryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found for appointment: " + appointmentId));

        // UPDATE 3: Consultation must actually be IN_PROGRESS before completion.
        // Do NOT allow WAITING -> COMPLETED or CALLED -> COMPLETED. Only IN_PROGRESS -> COMPLETED.
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS ||
            queueEntry.getQueueStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Consultation must be IN_PROGRESS before completion. Current status: appointment=" +
                    appointment.getStatus() + ", queue=" + queueEntry.getQueueStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        queueEntry.setQueueStatus(QueueStatus.COMPLETED);

        appointmentRepository.save(appointment);
        queueEntryRepository.save(queueEntry);

        log.info("Completed consultation for appointment {} with patient {}",
                appointmentId, appointment.getPatient().getUser().getName());

        return mapToQueueItemResponse(queueEntry, 0);
    }

    /**
     * UPDATE 4: Show patient's complete current queue status.
     * Prefers latest active appointment (WAITING, CALLED, IN_PROGRESS).
     * If none active, shows latest completed/cancelled appointment.
     * Returns meaningful status and position (0 for CALLED, IN_PROGRESS, COMPLETED, CANCELLED).
     */
    public PatientQueueStatusResponse getPatientQueueStatus(Long patientId) {
        List<Appointment> patientAppts = appointmentRepository.findByPatientId(patientId);
        if (patientAppts.isEmpty()) {
            return null;
        }

        // Prefer latest active appointment (WAITING, IN_PROGRESS, BOOKED)
        Optional<Appointment> activeApptOpt = patientAppts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.WAITING ||
                             a.getStatus() == AppointmentStatus.IN_PROGRESS ||
                             a.getStatus() == AppointmentStatus.BOOKED)
                .max(Comparator.comparing(Appointment::getCreatedAt));

        // If no active appointment, pick the latest completed or cancelled appointment
        Appointment appt = activeApptOpt.orElseGet(() ->
                patientAppts.stream()
                        .max(Comparator.comparing(Appointment::getCreatedAt))
                        .orElse(null)
        );

        if (appt == null) {
            return null;
        }

        QueueEntry patientEntry = queueEntryRepository.findByAppointmentId(appt.getId()).orElse(null);

        PatientQueueStatusResponse response = new PatientQueueStatusResponse();
        response.setAppointmentId(appt.getId());
        response.setDoctorName(appt.getDoctor().getUser().getName());
        response.setDepartmentName(appt.getDoctor().getDepartment().getName());
        response.setPriorityType(appt.getPriorityType());
        response.setAppointmentDate(appt.getAppointmentDate());
        response.setAppointmentTime(appt.getAppointmentTime());
        response.setAppointmentStatus(appt.getStatus());

        if (patientEntry != null) {
            response.setPriorityScore(patientEntry.getPriorityScore());
            response.setQueueStatus(patientEntry.getQueueStatus());
        } else {
            response.setPriorityScore(appt.getPriorityType() != null ? appt.getPriorityType().getDefaultScore() : 20);
            if (appt.getStatus() == AppointmentStatus.COMPLETED) {
                response.setQueueStatus(QueueStatus.COMPLETED);
            } else if (appt.getStatus() == AppointmentStatus.CANCELLED) {
                response.setQueueStatus(QueueStatus.SKIPPED);
            } else {
                response.setQueueStatus(QueueStatus.WAITING);
            }
        }

        // Populate schedule details and capacity info
        doctorScheduleRepository.findByDoctorIdAndAvailableDate(appt.getDoctor().getId(), appt.getAppointmentDate())
                .ifPresent(schedule -> {
                    response.setScheduleStartTime(schedule.getStartTime());
                    response.setScheduleEndTime(schedule.getEndTime());
                    response.setScheduleTime(schedule.getStartTime() + " - " + schedule.getEndTime());
                    response.setScheduleCapacity(schedule.getMaxAppointments());
                });

        long bookedCapacity = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(
                appt.getDoctor().getId(), appt.getAppointmentDate(), AppointmentStatus.CANCELLED
        );
        response.setBookedCapacity(bookedCapacity);

        List<QueueEntry> waiting = queueEntryRepository.findByDoctorIdAndDateAndStatus(
                appt.getDoctor().getId(), appt.getAppointmentDate(), QueueStatus.WAITING
        );
        List<QueueEntry> activeWaiting = waiting.stream()
                .filter(q -> q.getAppointment().getStatus() != AppointmentStatus.CANCELLED
                          && q.getAppointment().getStatus() != AppointmentStatus.COMPLETED)
                .toList();
        response.setPatientsWaiting(activeWaiting.size());
        response.setQueueSize(activeWaiting.size());

        QueueStatus effectiveQueueStatus = response.getQueueStatus();

        if (effectiveQueueStatus == QueueStatus.COMPLETED || appt.getStatus() == AppointmentStatus.COMPLETED) {
            response.setQueueStatus(QueueStatus.COMPLETED);
            response.setAppointmentStatus(AppointmentStatus.COMPLETED);
            response.setQueuePosition(0);
            response.setPatientsAhead(0);
            response.setStatusMessage("Consultation completed.");
        } else if (effectiveQueueStatus == QueueStatus.SKIPPED || appt.getStatus() == AppointmentStatus.CANCELLED) {
            response.setQueueStatus(QueueStatus.SKIPPED);
            response.setAppointmentStatus(AppointmentStatus.CANCELLED);
            response.setQueuePosition(0);
            response.setPatientsAhead(0);
            response.setStatusMessage("Appointment cancelled.");
        } else if (effectiveQueueStatus == QueueStatus.IN_PROGRESS || appt.getStatus() == AppointmentStatus.IN_PROGRESS) {
            response.setQueueStatus(QueueStatus.IN_PROGRESS);
            response.setAppointmentStatus(AppointmentStatus.IN_PROGRESS);
            response.setQueuePosition(0);
            response.setPatientsAhead(0);
            response.setStatusMessage("Consultation in progress.");
        } else if (effectiveQueueStatus == QueueStatus.CALLED) {
            response.setQueueStatus(QueueStatus.CALLED);
            response.setQueuePosition(0);
            response.setPatientsAhead(0);
            response.setStatusMessage("Please proceed to consultation.");
        } else if (effectiveQueueStatus == QueueStatus.WAITING) {
            response.setStatusMessage("Waiting for consultation.");
            if (patientEntry != null) {
                PriorityQueue<QueueEntry> pq = new PriorityQueue<>(PRIORITY_COMPARATOR);
                pq.addAll(activeWaiting);

                int pos = 1;
                int foundPos = 1;
                while (!pq.isEmpty()) {
                    QueueEntry entry = pq.poll();
                    if (entry.getId().equals(patientEntry.getId())) {
                        foundPos = pos;
                        break;
                    }
                    pos++;
                }

                response.setQueuePosition(foundPos);
                response.setPatientsAhead(Math.max(0, foundPos - 1));
            } else {
                response.setQueuePosition(1);
                response.setPatientsAhead(0);
            }
        } else {
            response.setQueuePosition(0);
            response.setPatientsAhead(0);
            response.setStatusMessage("Status: " + effectiveQueueStatus);
        }

        return response;
    }

    public QueueItemResponse mapToQueueItemResponse(QueueEntry entry, int position) {
        QueueItemResponse item = new QueueItemResponse();
        item.setPosition(position);
        item.setQueueEntryId(entry.getId());
        item.setAppointmentId(entry.getAppointment().getId());
        item.setPatientId(entry.getAppointment().getPatient().getId());
        item.setPatientName(entry.getAppointment().getPatient().getUser().getName());
        item.setPatientAge(entry.getAppointment().getPatient().getAge());
        item.setPatientGender(entry.getAppointment().getPatient().getGender());
        item.setPriorityType(entry.getAppointment().getPriorityType());
        item.setPriorityScore(entry.getPriorityScore());
        item.setAppointmentTime(entry.getAppointment().getAppointmentTime());
        item.setArrivalTime(entry.getArrivalTime());
        item.setQueueStatus(entry.getQueueStatus());
        item.setSymptoms(entry.getAppointment().getSymptoms());
        return item;
    }
}
