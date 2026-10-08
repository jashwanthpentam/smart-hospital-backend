package com.hospital;

import com.hospital.dto.AppointmentRequest;
import com.hospital.dto.AppointmentResponse;
import com.hospital.dto.PatientQueueStatusResponse;
import com.hospital.dto.QueueItemResponse;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ConflictException;
import com.hospital.repository.*;
import com.hospital.service.AppointmentService;
import com.hospital.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VerificationIntegrationTest {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verification Step 1-3: Verify 30-day schedules exist for doctors")
    void testDoctorSchedulesForNext30Days() {
        List<Doctor> doctors = doctorRepository.findAll();
        assertFalse(doctors.isEmpty(), "Doctors should exist");

        LocalDate today = LocalDate.now();
        for (Doctor doc : doctors) {
            // Check day 0, day 15, day 30
            assertTrue(scheduleRepository.existsByDoctorIdAndAvailableDate(doc.getId(), today),
                    "Doctor " + doc.getId() + " should have schedule for today");
            assertTrue(scheduleRepository.existsByDoctorIdAndAvailableDate(doc.getId(), today.plusDays(15)),
                    "Doctor " + doc.getId() + " should have schedule for today + 15 days");
            assertTrue(scheduleRepository.existsByDoctorIdAndAvailableDate(doc.getId(), today.plusDays(30)),
                    "Doctor " + doc.getId() + " should have schedule for today + 30 days");
        }
    }

    @Test
    @DisplayName("Verification Step 4: Confirm duplicate doctor slot booking is rejected")
    @Transactional
    void testDuplicateDoctorSlotRejected() {
        Doctor doctor = doctorRepository.findAll().getFirst();
        Patient patient = patientRepository.findAll().getFirst();

        LocalDate testDate = LocalDate.now().plusDays(2);
        LocalTime testTime = LocalTime.of(15, 0); // Dr. Sarah's schedule: 15:00–20:00

        AppointmentRequest req1 = new AppointmentRequest();
        req1.setDoctorId(doctor.getId());
        req1.setPatientId(patient.getId());
        req1.setAppointmentDate(testDate);
        req1.setAppointmentTime(testTime);
        req1.setPriorityType(PriorityType.NORMAL);
        req1.setSymptoms("First patient visit");

        AppointmentResponse resp1 = appointmentService.bookAppointment(req1, patient.getId());
        assertNotNull(resp1);

        // Try booking same doctor, same date, same time -> Should throw ConflictException
        AppointmentRequest req2 = new AppointmentRequest();
        req2.setDoctorId(doctor.getId());
        req2.setPatientId(patient.getId());
        req2.setAppointmentDate(testDate);
        req2.setAppointmentTime(testTime);
        req2.setPriorityType(PriorityType.URGENT);
        req2.setSymptoms("Second patient visit");

        assertThrows(ConflictException.class, () ->
                appointmentService.bookAppointment(req2, patient.getId())
        );
    }

    @Test
    @DisplayName("Verification Step 5-14: Full Queue Workflow (EMERGENCY > URGENT > NORMAL, CALL NEXT, START, COMPLETE, Patient Status)")
    @Transactional
    void testCompleteQueueWorkflow() {
        Doctor doctor = doctorRepository.findAll().getFirst();
        LocalDate testDate = LocalDate.now().plusDays(5);

        // Create 3 distinct patients
        User u1 = userRepository.save(new User(null, "Patient Normal", "p_normal_" + System.currentTimeMillis() + "@test.com", "pass", Role.PATIENT, java.time.LocalDateTime.now()));
        Patient patientNormal = patientRepository.save(new Patient(null, u1, "111111", 25, Gender.MALE));

        User u2 = userRepository.save(new User(null, "Patient Urgent", "p_urgent_" + System.currentTimeMillis() + "@test.com", "pass", Role.PATIENT, java.time.LocalDateTime.now()));
        Patient patientUrgent = patientRepository.save(new Patient(null, u2, "222222", 40, Gender.FEMALE));

        User u3 = userRepository.save(new User(null, "Patient Emergency", "p_emergency_" + System.currentTimeMillis() + "@test.com", "pass", Role.PATIENT, java.time.LocalDateTime.now()));
        Patient patientEmergency = patientRepository.save(new Patient(null, u3, "333333", 60, Gender.MALE));

        // 1. Create NORMAL patient appointment
        AppointmentRequest reqNormal = new AppointmentRequest();
        reqNormal.setDoctorId(doctor.getId());
        reqNormal.setPatientId(patientNormal.getId());
        reqNormal.setAppointmentDate(testDate);
        reqNormal.setAppointmentTime(LocalTime.of(10, 0));
        reqNormal.setPriorityType(PriorityType.NORMAL);
        reqNormal.setSymptoms("Mild cough");
        AppointmentResponse normalAppt = appointmentService.bookAppointment(reqNormal, patientNormal.getId());

        // 2. Create URGENT patient appointment (at 10:15)
        AppointmentRequest reqUrgent = new AppointmentRequest();
        reqUrgent.setDoctorId(doctor.getId());
        reqUrgent.setPatientId(patientUrgent.getId());
        reqUrgent.setAppointmentDate(testDate);
        reqUrgent.setAppointmentTime(LocalTime.of(10, 15));
        reqUrgent.setPriorityType(PriorityType.URGENT);
        reqUrgent.setSymptoms("High fever");
        AppointmentResponse urgentAppt = appointmentService.bookAppointment(reqUrgent, patientUrgent.getId());

        // 3. Create EMERGENCY patient appointment (at 10:30)
        AppointmentRequest reqEmergency = new AppointmentRequest();
        reqEmergency.setDoctorId(doctor.getId());
        reqEmergency.setPatientId(patientEmergency.getId());
        reqEmergency.setAppointmentDate(testDate);
        reqEmergency.setAppointmentTime(LocalTime.of(10, 30));
        reqEmergency.setPriorityType(PriorityType.EMERGENCY);
        reqEmergency.setSymptoms("Chest pain");
        AppointmentResponse emergencyAppt = appointmentService.bookAppointment(reqEmergency, patientEmergency.getId());

        // 4. Verify queue order is EMERGENCY (100) -> URGENT (50) -> NORMAL (20)
        List<QueueItemResponse> queue = queueService.getDoctorQueue(doctor.getId(), testDate);
        assertEquals(3, queue.size(), "All 3 appointments should be waiting in queue");
        assertEquals(PriorityType.EMERGENCY, queue.get(0).getPriorityType(), "Top queue item must be EMERGENCY");
        assertEquals(PriorityType.URGENT, queue.get(1).getPriorityType(), "Second queue item must be URGENT");
        assertEquals(PriorityType.NORMAL, queue.get(2).getPriorityType(), "Third queue item must be NORMAL");

        // Verify patient waiting positions
        PatientQueueStatusResponse statusWaitingEmergency = queueService.getPatientQueueStatus(patientEmergency.getId());
        assertNotNull(statusWaitingEmergency);
        assertEquals(1, statusWaitingEmergency.getQueuePosition(), "Emergency patient is position 1");
        assertEquals(0, statusWaitingEmergency.getPatientsAhead());

        PatientQueueStatusResponse statusWaitingUrgent = queueService.getPatientQueueStatus(patientUrgent.getId());
        assertEquals(2, statusWaitingUrgent.getQueuePosition(), "Urgent patient is position 2");
        assertEquals(1, statusWaitingUrgent.getPatientsAhead());

        // 5. Test Step A: Doctor CANNOT start consultation directly from WAITING
        assertThrows(BadRequestException.class, () ->
                queueService.startConsultation(emergencyAppt.getId(), doctor.getId()),
                "Doctor must not be able to start consultation directly from WAITING"
        );

        // 6. Call next patient: WAITING -> CALLED
        QueueItemResponse calledItem = queueService.callNextPatient(doctor.getId(), testDate);
        assertEquals(emergencyAppt.getId(), calledItem.getAppointmentId());
        assertEquals(QueueStatus.CALLED, calledItem.getQueueStatus());

        // Check patient queue status during CALLED: queuePosition = 0, patientsAhead = 0
        PatientQueueStatusResponse statusCalled = queueService.getPatientQueueStatus(patientEmergency.getId());
        assertNotNull(statusCalled);
        assertEquals(QueueStatus.CALLED, statusCalled.getQueueStatus());
        assertEquals(0, statusCalled.getQueuePosition());
        assertEquals(0, statusCalled.getPatientsAhead());

        // 7. Test Step B: Cannot complete consultation while CALLED (must be IN_PROGRESS)
        assertThrows(BadRequestException.class, () ->
                queueService.completeConsultation(emergencyAppt.getId(), doctor.getId()),
                "Cannot complete consultation directly from CALLED"
        );

        // 8. Start consultation: CALLED -> IN_PROGRESS
        QueueItemResponse startedItem = queueService.startConsultation(emergencyAppt.getId(), doctor.getId());
        assertEquals(QueueStatus.IN_PROGRESS, startedItem.getQueueStatus());

        // Check patient queue status during IN_PROGRESS: queuePosition = 0, patientsAhead = 0
        PatientQueueStatusResponse statusInProgress = queueService.getPatientQueueStatus(patientEmergency.getId());
        assertEquals(QueueStatus.IN_PROGRESS, statusInProgress.getQueueStatus());
        assertEquals(0, statusInProgress.getQueuePosition());

        // 9. Complete consultation: IN_PROGRESS -> COMPLETED
        QueueItemResponse completedItem = queueService.completeConsultation(emergencyAppt.getId(), doctor.getId());
        assertEquals(QueueStatus.COMPLETED, completedItem.getQueueStatus());

        // 10. Check patient queue status after completion: COMPLETED, position = 0, patientsAhead = 0
        PatientQueueStatusResponse statusCompleted = queueService.getPatientQueueStatus(patientEmergency.getId());
        assertNotNull(statusCompleted);
        assertEquals(QueueStatus.COMPLETED, statusCompleted.getQueueStatus());
        assertEquals(AppointmentStatus.COMPLETED, statusCompleted.getAppointmentStatus());
        assertEquals(0, statusCompleted.getQueuePosition());
        assertEquals(0, statusCompleted.getPatientsAhead());

        // 11. Cancel another waiting appointment (normalAppt) -> CANCELLED / SKIPPED
        appointmentService.cancelAppointment(normalAppt.getId(), patientNormal.getId(), true);
        Appointment cancelledAppt = appointmentRepository.findById(normalAppt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, cancelledAppt.getStatus());

        QueueEntry cancelledEntry = queueEntryRepository.findByAppointmentId(normalAppt.getId()).orElseThrow();
        assertEquals(QueueStatus.SKIPPED, cancelledEntry.getQueueStatus());

        // Check cancelled patient status -> SKIPPED/CANCELLED, position = 0
        PatientQueueStatusResponse statusCancelled = queueService.getPatientQueueStatus(patientNormal.getId());
        assertNotNull(statusCancelled);
        assertEquals(QueueStatus.SKIPPED, statusCancelled.getQueueStatus());
        assertEquals(AppointmentStatus.CANCELLED, statusCancelled.getAppointmentStatus());
        assertEquals(0, statusCancelled.getQueuePosition());
    }

    @Test
    @DisplayName("Verification Step 15: Verify doctor_details and patient_details database views")
    void testDatabaseViews() {
        // Query doctor_details view
        List<Map<String, Object>> doctorRows = jdbcTemplate.queryForList("SELECT * FROM doctor_details");
        assertFalse(doctorRows.isEmpty(), "doctor_details view should contain records");
        Map<String, Object> firstDoc = doctorRows.getFirst();
        assertNotNull(firstDoc.get("doctor_name"), "doctor_details must expose doctor_name");
        assertNotNull(firstDoc.get("department_name"), "doctor_details must expose department_name");
        assertNotNull(firstDoc.get("email"), "doctor_details must expose email");

        // Query patient_details view
        List<Map<String, Object>> patientRows = jdbcTemplate.queryForList("SELECT * FROM patient_details");
        assertFalse(patientRows.isEmpty(), "patient_details view should contain records");
        Map<String, Object> firstPat = patientRows.getFirst();
        assertNotNull(firstPat.get("patient_name"), "patient_details must expose patient_name");
        assertNotNull(firstPat.get("phone"), "patient_details must expose phone");
        assertNotNull(firstPat.get("email"), "patient_details must expose email");
    }

    @Test
    @DisplayName("Verification Step 16: Invalid schedule date rejected by backend")
    void testInvalidScheduleDateRejected() {
        Doctor doctor = doctorRepository.findAll().getFirst();
        Patient patient = patientRepository.findAll().getFirst();
        LocalDate unscheduledDate = LocalDate.now().plusDays(60);

        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctor.getId());
        req.setPatientId(patient.getId());
        req.setAppointmentDate(unscheduledDate);
        req.setPriorityType(PriorityType.NORMAL);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.bookAppointment(req, patient.getId())
        );
        assertTrue(ex.getMessage().contains("no schedule available"));
    }

    @Test
    @DisplayName("Verification Step 17: Live Queue - New emergency dynamically shifts waiting patient position")
    @Transactional
    void testDynamicQueueShiftOnNewEmergencyArrival() {
        Doctor doctor = doctorRepository.findAll().getFirst();
        LocalDate testDate = LocalDate.now().plusDays(7);

        User u1 = userRepository.save(new User(null, "Patient One", "p1_" + System.currentTimeMillis() + "@test.com", "pass", Role.PATIENT, java.time.LocalDateTime.now()));
        Patient patient1 = patientRepository.save(new Patient(null, u1, "444444", 25, Gender.MALE));

        User u2 = userRepository.save(new User(null, "Patient Emergency Two", "p2_" + System.currentTimeMillis() + "@test.com", "pass", Role.PATIENT, java.time.LocalDateTime.now()));
        Patient patient2 = patientRepository.save(new Patient(null, u2, "555555", 35, Gender.FEMALE));

        // 1. Patient 1 (NORMAL) books
        AppointmentRequest req1 = new AppointmentRequest();
        req1.setDoctorId(doctor.getId());
        req1.setPatientId(patient1.getId());
        req1.setAppointmentDate(testDate);
        req1.setPriorityType(PriorityType.NORMAL);
        appointmentService.bookAppointment(req1, patient1.getId());

        // Patient 1 sees position 1, 0 ahead
        PatientQueueStatusResponse statusBefore = queueService.getPatientQueueStatus(patient1.getId());
        assertEquals(1, statusBefore.getQueuePosition());
        assertEquals(0, statusBefore.getPatientsAhead());

        // 2. Patient 2 (EMERGENCY) arrives later
        AppointmentRequest req2 = new AppointmentRequest();
        req2.setDoctorId(doctor.getId());
        req2.setPatientId(patient2.getId());
        req2.setAppointmentDate(testDate);
        req2.setPriorityType(PriorityType.EMERGENCY);
        appointmentService.bookAppointment(req2, patient2.getId());

        // Patient 1 position automatically shifts to 2, 1 ahead
        PatientQueueStatusResponse statusAfter = queueService.getPatientQueueStatus(patient1.getId());
        assertEquals(2, statusAfter.getQueuePosition(), "Normal patient dynamically shifted behind emergency patient");
        assertEquals(1, statusAfter.getPatientsAhead());
    }
}
