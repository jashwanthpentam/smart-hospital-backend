package com.hospital;

import com.hospital.dto.QueueItemResponse;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.QueueEntryRepository;
import com.hospital.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueueServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private com.hospital.repository.DoctorScheduleRepository doctorScheduleRepository;

    @InjectMocks
    private QueueService queueService;

    private Doctor testDoctor;
    private Patient patientA;
    private Patient patientB;
    private Patient patientC;
    private Patient patientD;

    @BeforeEach
    void setUp() {
        Department dept = new Department(1L, "Cardiology", "Heart care");

        User docUser = new User(1L, "Dr. Smith", "doc@hospital.com", "pass", Role.DOCTOR, LocalDateTime.now());
        testDoctor = new Doctor(1L, docUser, dept, "Cardiology", 10);

        User u1 = new User(2L, "Alice", "alice@test.com", "pass", Role.PATIENT, LocalDateTime.now());
        patientA = new Patient(1L, u1, "123456", 25, Gender.FEMALE);

        User u2 = new User(3L, "Bob", "bob@test.com", "pass", Role.PATIENT, LocalDateTime.now());
        patientB = new Patient(2L, u2, "234567", 40, Gender.MALE);

        User u3 = new User(4L, "Charlie", "charlie@test.com", "pass", Role.PATIENT, LocalDateTime.now());
        patientC = new Patient(3L, u3, "345678", 55, Gender.MALE);

        User u4 = new User(5L, "David", "david@test.com", "pass", Role.PATIENT, LocalDateTime.now());
        patientD = new Patient(4L, u4, "456789", 30, Gender.MALE);
    }

    @Test
    @DisplayName("PriorityQueue ordering: EMERGENCY (100) > URGENT (50) > NORMAL (20)")
    void testPriorityQueueOrdering() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 9, 0);

        Appointment apptNormal = new Appointment(1L, patientA, testDoctor, LocalDate.now(), LocalTime.of(10, 0), PriorityType.NORMAL, "Checkup", AppointmentStatus.WAITING, baseTime);
        QueueEntry qNormal = new QueueEntry(1L, apptNormal, 20, QueueStatus.WAITING, baseTime.plusMinutes(5));

        Appointment apptUrgent = new Appointment(2L, patientB, testDoctor, LocalDate.now(), LocalTime.of(10, 15), PriorityType.URGENT, "Fever", AppointmentStatus.WAITING, baseTime);
        QueueEntry qUrgent = new QueueEntry(2L, apptUrgent, 50, QueueStatus.WAITING, baseTime.plusMinutes(10));

        Appointment apptEmergency = new Appointment(3L, patientC, testDoctor, LocalDate.now(), LocalTime.of(10, 30), PriorityType.EMERGENCY, "Trauma", AppointmentStatus.WAITING, baseTime);
        QueueEntry qEmergency = new QueueEntry(3L, apptEmergency, 100, QueueStatus.WAITING, baseTime.plusMinutes(15));

        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(QueueService.PRIORITY_COMPARATOR);
        pq.add(qNormal);
        pq.add(qUrgent);
        pq.add(qEmergency);

        assertEquals(100, pq.poll().getPriorityScore(), "First out must be EMERGENCY (100)");
        assertEquals(50, pq.poll().getPriorityScore(), "Second out must be URGENT (50)");
        assertEquals(20, pq.poll().getPriorityScore(), "Third out must be NORMAL (20)");
    }

    @Test
    @DisplayName("Tie-breaker: Same priority uses earlier arrival time first")
    void testSamePriorityEarlierArrivalFirst() {
        LocalDateTime timeEarlier = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime timeLater = LocalDateTime.of(2026, 10, 1, 9, 30);

        Appointment appt1 = new Appointment(1L, patientA, testDoctor, LocalDate.now(), LocalTime.of(10, 0), PriorityType.NORMAL, "Checkup", AppointmentStatus.WAITING, timeEarlier);
        QueueEntry earlierEntry = new QueueEntry(1L, appt1, 20, QueueStatus.WAITING, timeEarlier);

        Appointment appt2 = new Appointment(2L, patientB, testDoctor, LocalDate.now(), LocalTime.of(10, 30), PriorityType.NORMAL, "Checkup 2", AppointmentStatus.WAITING, timeLater);
        QueueEntry laterEntry = new QueueEntry(2L, appt2, 20, QueueStatus.WAITING, timeLater);

        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(QueueService.PRIORITY_COMPARATOR);
        pq.add(laterEntry);
        pq.add(earlierEntry);

        QueueEntry first = pq.poll();
        assertEquals(1L, first.getId(), "Earlier arrival entry must be polled first");
        assertEquals(2L, pq.poll().getId(), "Later arrival entry must be polled second");
    }

    @Test
    @DisplayName("Queue excludes CANCELLED and COMPLETED appointments")
    void testExcludesCancelledAndCompleted() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Appointment apptValid = new Appointment(1L, patientA, testDoctor, today, LocalTime.of(10, 0), PriorityType.NORMAL, "OK", AppointmentStatus.WAITING, now);
        QueueEntry qValid = new QueueEntry(1L, apptValid, 20, QueueStatus.WAITING, now);

        Appointment apptCancelled = new Appointment(2L, patientB, testDoctor, today, LocalTime.of(10, 30), PriorityType.EMERGENCY, "Cancelled", AppointmentStatus.CANCELLED, now);
        QueueEntry qCancelled = new QueueEntry(2L, apptCancelled, 100, QueueStatus.WAITING, now);

        Appointment apptCompleted = new Appointment(3L, patientC, testDoctor, today, LocalTime.of(11, 0), PriorityType.URGENT, "Done", AppointmentStatus.COMPLETED, now);
        QueueEntry qCompleted = new QueueEntry(3L, apptCompleted, 50, QueueStatus.WAITING, now);

        when(queueEntryRepository.findByDoctorIdAndDateAndStatus(1L, today, QueueStatus.WAITING))
                .thenReturn(List.of(qValid, qCancelled, qCompleted));

        List<QueueItemResponse> queue = queueService.getDoctorQueue(1L, today);

        assertEquals(1, queue.size(), "Queue must only contain active, non-cancelled, non-completed appointments");
        assertEquals(1L, queue.getFirst().getQueueEntryId());
    }

    @Test
    @DisplayName("Doctor cannot call next patient when consultation is IN_PROGRESS")
    void testCannotCallNextWhileConsultationInProgress() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Appointment activeAppt = new Appointment(1L, patientA, testDoctor, today, LocalTime.of(10, 0), PriorityType.NORMAL, "In progress", AppointmentStatus.IN_PROGRESS, now);
        QueueEntry activeQueue = new QueueEntry(1L, activeAppt, 20, QueueStatus.IN_PROGRESS, now);

        when(queueEntryRepository.findActiveByDoctorIdAndDate(1L, today, List.of(QueueStatus.IN_PROGRESS)))
                .thenReturn(List.of(activeQueue));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                queueService.callNextPatient(1L, today)
        );

        assertTrue(ex.getMessage().contains("IN_PROGRESS"), "Should mention IN_PROGRESS conflict");
    }

    @Test
    @DisplayName("UPDATE 3: Cannot start consultation directly from WAITING; patient must be CALLED")
    void testStartConsultationRequiresCalled() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appt = new Appointment(1L, patientA, testDoctor, LocalDate.now(), LocalTime.of(10, 0), PriorityType.NORMAL, "Checkup", AppointmentStatus.WAITING, now);
        QueueEntry waitingQueue = new QueueEntry(1L, appt, 20, QueueStatus.WAITING, now);

        when(appointmentRepository.findById(1L)).thenReturn(java.util.Optional.of(appt));
        when(queueEntryRepository.findByAppointmentId(1L)).thenReturn(java.util.Optional.of(waitingQueue));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                queueService.startConsultation(1L, testDoctor.getId())
        );

        assertTrue(ex.getMessage().contains("CALLED"), "Must state patient must be CALLED first");
    }

    @Test
    @DisplayName("UPDATE 3: Consultation must be IN_PROGRESS before completeConsultation")
    void testCompleteConsultationRequiresInProgress() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appt = new Appointment(1L, patientA, testDoctor, LocalDate.now(), LocalTime.of(10, 0), PriorityType.NORMAL, "Checkup", AppointmentStatus.WAITING, now);
        QueueEntry calledQueue = new QueueEntry(1L, appt, 20, QueueStatus.CALLED, now);

        when(appointmentRepository.findById(1L)).thenReturn(java.util.Optional.of(appt));
        when(queueEntryRepository.findByAppointmentId(1L)).thenReturn(java.util.Optional.of(calledQueue));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                queueService.completeConsultation(1L, testDoctor.getId())
        );

        assertTrue(ex.getMessage().contains("IN_PROGRESS"), "Must state consultation must be IN_PROGRESS");
    }

    @Test
    @DisplayName("UPDATE 4: Patient queue status shows COMPLETED when visit is done")
    void testPatientQueueStatusShowsCompleted() {
        LocalDateTime now = LocalDateTime.now();
        Appointment completedAppt = new Appointment(1L, patientA, testDoctor, LocalDate.now(), LocalTime.of(10, 0), PriorityType.NORMAL, "Checkup", AppointmentStatus.COMPLETED, now);
        QueueEntry completedQueue = new QueueEntry(1L, completedAppt, 20, QueueStatus.COMPLETED, now);

        when(appointmentRepository.findByPatientId(patientA.getId())).thenReturn(List.of(completedAppt));
        when(queueEntryRepository.findByAppointmentId(1L)).thenReturn(java.util.Optional.of(completedQueue));

        var statusResponse = queueService.getPatientQueueStatus(patientA.getId());

        assertNotNull(statusResponse, "Response must not be null for completed visit");
        assertEquals(QueueStatus.COMPLETED, statusResponse.getQueueStatus());
        assertEquals(AppointmentStatus.COMPLETED, statusResponse.getAppointmentStatus());
        assertEquals(0, statusResponse.getQueuePosition());
        assertEquals(0, statusResponse.getPatientsAhead());
    }

    @Test
    @DisplayName("PHASE 2: Stable Priority Queue T1/T2 ordering: E1, E6, E7, E8, U2, U9, U10, N3, N4, N5, N11, N12")
    void testStablePriorityQueueOrderingT1T2() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 10, 9, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 10, 9, 30, 0);

        // At T1: E1, U2, N3, N4, N5
        QueueEntry e1 = new QueueEntry(1L, null, 100, QueueStatus.WAITING, t1);
        QueueEntry u2 = new QueueEntry(2L, null, 50, QueueStatus.WAITING, t1);
        QueueEntry n3 = new QueueEntry(3L, null, 20, QueueStatus.WAITING, t1);
        QueueEntry n4 = new QueueEntry(4L, null, 20, QueueStatus.WAITING, t1.plusSeconds(1));
        QueueEntry n5 = new QueueEntry(5L, null, 20, QueueStatus.WAITING, t1.plusSeconds(2));

        // At T2: E6, E7, E8, U9, U10, N11, N12
        QueueEntry e6 = new QueueEntry(6L, null, 100, QueueStatus.WAITING, t2);
        QueueEntry e7 = new QueueEntry(7L, null, 100, QueueStatus.WAITING, t2.plusSeconds(1));
        QueueEntry e8 = new QueueEntry(8L, null, 100, QueueStatus.WAITING, t2.plusSeconds(2));
        QueueEntry u9 = new QueueEntry(9L, null, 50, QueueStatus.WAITING, t2);
        QueueEntry u10 = new QueueEntry(10L, null, 50, QueueStatus.WAITING, t2.plusSeconds(1));
        QueueEntry n11 = new QueueEntry(11L, null, 20, QueueStatus.WAITING, t2);
        QueueEntry n12 = new QueueEntry(12L, null, 20, QueueStatus.WAITING, t2.plusSeconds(1));

        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(QueueService.PRIORITY_COMPARATOR);
        pq.addAll(List.of(e1, u2, n3, n4, n5, e6, e7, e8, u9, u10, n11, n12));

        List<Long> orderedIds = new ArrayList<>();
        while (!pq.isEmpty()) {
            orderedIds.add(pq.poll().getId());
        }

        List<Long> expectedIds = List.of(1L, 6L, 7L, 8L, 2L, 9L, 10L, 3L, 4L, 5L, 11L, 12L);
        assertEquals(expectedIds, orderedIds, "PriorityQueue must match exact stable order: E1, E6, E7, E8, U2, U9, U10, N3, N4, N5, N11, N12");
    }
}
