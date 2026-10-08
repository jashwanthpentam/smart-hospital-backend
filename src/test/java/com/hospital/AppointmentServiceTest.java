package com.hospital;

import com.hospital.dto.AppointmentRequest;
import com.hospital.dto.AppointmentResponse;
import com.hospital.entity.*;
import com.hospital.exception.ConflictException;
import com.hospital.repository.*;
import com.hospital.service.AppointmentService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Doctor testDoctor;
    private Patient testPatient;
    private DoctorSchedule testSchedule;

    @BeforeEach
    void setUp() {
        Department dept = new Department(1L, "Cardiology", "Heart care");
        User docUser = new User(1L, "Dr. Sarah", "sarah@test.com", "pass", Role.DOCTOR, LocalDateTime.now());
        testDoctor = new Doctor(1L, docUser, dept, "Cardiology", 10);

        User patientUser = new User(2L, "John Doe", "john@test.com", "pass", Role.PATIENT, LocalDateTime.now());
        testPatient = new Patient(1L, patientUser, "1234567890", 30, Gender.MALE);

        testSchedule = new DoctorSchedule(1L, testDoctor, LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), 15);
    }

    @Test
    @DisplayName("UPDATE 2: Rejects booking if doctor already has an active appointment at exact date and time")
    void testRejectsDoctorSlotDoubleBooking() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(10, 0);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(testPatient.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setAppointmentTime(time);
        request.setPriorityType(PriorityType.NORMAL);
        request.setSymptoms("Routine check");

        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(testSchedule));
        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED)).thenReturn(2L);

        // Doctor slot IS already booked
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(testDoctor.getId(), date, time, AppointmentStatus.CANCELLED)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                appointmentService.bookAppointment(request, testPatient.getId())
        );

        assertTrue(ex.getMessage().contains("already has an active appointment scheduled"),
                "Must reject with clear doctor slot double-booking message");
    }

    @Test
    @DisplayName("UPDATE 2: Allows booking when doctor slot is free")
    void testAllowsBookingWhenDoctorSlotFree() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(10, 0);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(testPatient.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setAppointmentTime(time);
        request.setPriorityType(PriorityType.NORMAL);
        request.setSymptoms("Routine check");

        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(testSchedule));
        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED)).thenReturn(2L);
        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(testPatient.getId(), date, time, AppointmentStatus.CANCELLED)).thenReturn(false);
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(testDoctor.getId(), date, time, AppointmentStatus.CANCELLED)).thenReturn(false);

        Appointment savedAppt = new Appointment(1L, testPatient, testDoctor, date, time, PriorityType.NORMAL, "Routine check", AppointmentStatus.WAITING, LocalDateTime.now());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppt);

        AppointmentResponse response = appointmentService.bookAppointment(request, testPatient.getId());

        assertNotNull(response);
        assertEquals(savedAppt.getId(), response.getId());
        assertEquals("Dr. Sarah", response.getDoctorName());
        assertEquals("John Doe", response.getPatientName());
    }

    @Test
    @DisplayName("PHASE 1: Rejects booking if schedule is full (capacity reached)")
    void testRejectsBookingWhenCapacityReached() {
        LocalDate date = LocalDate.now().plusDays(1);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(testPatient.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setPriorityType(PriorityType.NORMAL);
        request.setSymptoms("Routine check");

        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(testSchedule));
        // Max is 15, current booked is 15
        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED)).thenReturn(15L);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                appointmentService.bookAppointment(request, testPatient.getId())
        );

        assertTrue(ex.getMessage().contains("Schedule is full. New bookings are closed."),
                "Must reject with exact message 'Schedule is full. New bookings are closed.'");
    }

    @Test
    @DisplayName("PHASE 1: Rejects booking if within 6-hour booking cutoff")
    void testRejectsBookingInsideCutoff() {
        // Shift starts today in 2 hours -> cutoff passed
        LocalDate date = LocalDate.now();
        DoctorSchedule pastCutoffSchedule = new DoctorSchedule(2L, testDoctor, date, LocalTime.now().plusHours(2), LocalTime.now().plusHours(6), 15);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(testPatient.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setPriorityType(PriorityType.NORMAL);

        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(pastCutoffSchedule));

        com.hospital.exception.BadRequestException ex = assertThrows(com.hospital.exception.BadRequestException.class, () ->
                appointmentService.bookAppointment(request, testPatient.getId())
        );

        assertTrue(ex.getMessage().contains("Booking closed"), "Must state booking closed due to cutoff");
    }

    @Test
    @DisplayName("ONE-BOOKING-PER-SCHEDULE: Rejects second booking by same patient in same schedule")
    void testRejectsPatientDuplicateInSameSchedule() {
        LocalDate date = LocalDate.now().plusDays(1);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(testPatient.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setPriorityType(PriorityType.NORMAL);
        request.setSymptoms("Second visit");

        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(testSchedule));
        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED)).thenReturn(1L);
        // Patient already has an active booking in this schedule
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                testPatient.getId(), testDoctor.getId(), date,
                java.util.List.of(AppointmentStatus.WAITING, AppointmentStatus.BOOKED, AppointmentStatus.IN_PROGRESS)
        )).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                appointmentService.bookAppointment(request, testPatient.getId())
        );
        assertTrue(ex.getMessage().contains("You already have an appointment in this schedule."),
                "Must reject with 'You already have an appointment in this schedule.'");
    }

    @Test
    @DisplayName("ONE-BOOKING-PER-SCHEDULE: Allows booking after previous appointment in same schedule is CANCELLED")
    void testAllowsRebookingAfterCancellation() {
        LocalDate date = LocalDate.now().plusDays(1);
        // Auto-allocation picks schedule.startTime = 09:00 (first free slot, no occupied slots)
        LocalTime expectedSlot = LocalTime.of(9, 0);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(testPatient.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setPriorityType(PriorityType.NORMAL);
        request.setSymptoms("Rebooked after cancel");

        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(testSchedule));
        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED)).thenReturn(0L);
        // No active bookings — previous one was cancelled
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                testPatient.getId(), testDoctor.getId(), date,
                java.util.List.of(AppointmentStatus.WAITING, AppointmentStatus.BOOKED, AppointmentStatus.IN_PROGRESS)
        )).thenReturn(false);
        // Auto-allocation path: returns empty list so 09:00 slot is free
        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED))
                .thenReturn(java.util.List.of());

        Appointment saved = new Appointment(2L, testPatient, testDoctor, date, expectedSlot, PriorityType.NORMAL, "Rebooked after cancel", AppointmentStatus.WAITING, java.time.LocalDateTime.now());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentResponse response = appointmentService.bookAppointment(request, testPatient.getId());
        assertNotNull(response, "Should successfully book after cancelled appointment");
    }

    @Test
    @DisplayName("ONE-BOOKING-PER-SCHEDULE: Second patient can book same schedule independently")
    void testSecondPatientCanBookSameSchedule() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(9, 15); // second slot

        User patient2User = new User(3L, "Jane Smith", "jane@test.com", "pass", Role.PATIENT, java.time.LocalDateTime.now());
        Patient patient2 = new Patient(2L, patient2User, "9876543210", 25, Gender.FEMALE);

        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(patient2.getId());
        request.setDoctorId(testDoctor.getId());
        request.setAppointmentDate(date);
        request.setPriorityType(PriorityType.URGENT);
        request.setSymptoms("Acute pain");

        when(patientRepository.findById(patient2.getId())).thenReturn(Optional.of(patient2));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndAvailableDate(testDoctor.getId(), date)).thenReturn(Optional.of(testSchedule));
        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED)).thenReturn(1L);
        // Patient 2 has no booking in this schedule
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusIn(
                patient2.getId(), testDoctor.getId(), date,
                java.util.List.of(AppointmentStatus.WAITING, AppointmentStatus.BOOKED, AppointmentStatus.IN_PROGRESS)
        )).thenReturn(false);

        // Slot 09:00 already taken by patient1, patient2 gets 09:15
        Appointment existingAppt = new Appointment(1L, testPatient, testDoctor, date, LocalTime.of(9, 0), PriorityType.NORMAL, "First", AppointmentStatus.WAITING, java.time.LocalDateTime.now());
        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(testDoctor.getId(), date, AppointmentStatus.CANCELLED))
                .thenReturn(java.util.List.of(existingAppt));
        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                patient2.getId(), date, time, AppointmentStatus.CANCELLED)).thenReturn(false);

        Appointment saved = new Appointment(3L, patient2, testDoctor, date, time, PriorityType.URGENT, "Acute pain", AppointmentStatus.WAITING, java.time.LocalDateTime.now());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentResponse response = appointmentService.bookAppointment(request, patient2.getId());
        assertNotNull(response, "Second patient should be allowed to book the same schedule");
        assertEquals(patient2.getId(), response.getPatientId());
    }
}
