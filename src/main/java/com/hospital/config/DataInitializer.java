package com.hospital.config;

import com.hospital.entity.*;
import com.hospital.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository,
                           DoctorScheduleRepository scheduleRepository,
                           PasswordEncoder passwordEncoder,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.scheduleRepository = scheduleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDepartmentsAndDoctors();
        seedSamplePatient();
        ensureDoctorSchedulesForNext30Days();
        createDatabaseViews();
    }

    private void seedAdmin() {
        if (!userRepository.existsByEmail("admin@hospital.com")) {
            User admin = new User();
            admin.setName("Hospital Administrator");
            admin.setEmail("admin@hospital.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Default Admin account seeded: admin@hospital.com / admin123");
        }
    }

    private void seedDepartmentsAndDoctors() {
        Department cardiology = departmentRepository.findByNameIgnoreCase("Cardiology")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Cardiology", "Heart and cardiovascular system care")));

        Department neurology = departmentRepository.findByNameIgnoreCase("Neurology")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Neurology", "Brain, spine and nervous system disorders")));

        Department orthopedics = departmentRepository.findByNameIgnoreCase("Orthopedics")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Orthopedics", "Bone, joint and musculoskeletal healthcare")));

        Department generalMed = departmentRepository.findByNameIgnoreCase("General Medicine")
                .orElseGet(() -> departmentRepository.save(new Department(null, "General Medicine", "Primary healthcare and general medical consultations")));

        Department pediatrics = departmentRepository.findByNameIgnoreCase("Pediatrics")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Pediatrics", "Infant, child and adolescent medical care")));

        // Doctor 1: Dr. Sarah Jenkins (Cardiology)
        if (!userRepository.existsByEmail("dr.sarah@hospital.com")) {
            User user1 = new User();
            user1.setName("Dr. Sarah Jenkins");
            user1.setEmail("dr.sarah@hospital.com");
            user1.setPassword(passwordEncoder.encode("doctor123"));
            user1.setRole(Role.DOCTOR);
            User savedUser1 = userRepository.save(user1);

            Doctor doc1 = new Doctor();
            doc1.setUser(savedUser1);
            doc1.setDepartment(cardiology);
            doc1.setSpecialization("Interventional Cardiology");
            doc1.setExperienceYears(12);
            Doctor savedDoc1 = doctorRepository.save(doc1);

            // Add today's schedule
            seedDoctorSchedule(savedDoc1, LocalDate.now());
            seedDoctorSchedule(savedDoc1, LocalDate.now().plusDays(1));
            log.info("Doctor Dr. Sarah Jenkins seeded: dr.sarah@hospital.com / doctor123");
        }

        // Doctor 2: Dr. Robert Chen (Neurology)
        if (!userRepository.existsByEmail("dr.robert@hospital.com")) {
            User user2 = new User();
            user2.setName("Dr. Robert Chen");
            user2.setEmail("dr.robert@hospital.com");
            user2.setPassword(passwordEncoder.encode("doctor123"));
            user2.setRole(Role.DOCTOR);
            User savedUser2 = userRepository.save(user2);

            Doctor doc2 = new Doctor();
            doc2.setUser(savedUser2);
            doc2.setDepartment(neurology);
            doc2.setSpecialization("Neurovascular Care");
            doc2.setExperienceYears(9);
            Doctor savedDoc2 = doctorRepository.save(doc2);

            seedDoctorSchedule(savedDoc2, LocalDate.now());
            seedDoctorSchedule(savedDoc2, LocalDate.now().plusDays(1));
            log.info("Doctor Dr. Robert Chen seeded: dr.robert@hospital.com / doctor123");
        }

        // Doctor 3: Dr. Emily Davis (General Medicine)
        if (!userRepository.existsByEmail("dr.emily@hospital.com")) {
            User user3 = new User();
            user3.setName("Dr. Emily Davis");
            user3.setEmail("dr.emily@hospital.com");
            user3.setPassword(passwordEncoder.encode("doctor123"));
            user3.setRole(Role.DOCTOR);
            User savedUser3 = userRepository.save(user3);

            Doctor doc3 = new Doctor();
            doc3.setUser(savedUser3);
            doc3.setDepartment(generalMed);
            doc3.setSpecialization("Family Physician");
            doc3.setExperienceYears(7);
            Doctor savedDoc3 = doctorRepository.save(doc3);

            seedDoctorSchedule(savedDoc3, LocalDate.now());
            seedDoctorSchedule(savedDoc3, LocalDate.now().plusDays(1));
            log.info("Doctor Dr. Emily Davis seeded: dr.emily@hospital.com / doctor123");
        }
    }

    private DoctorSchedule buildScheduleForDoctor(Doctor doc, LocalDate date) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doc);
        schedule.setAvailableDate(date);

        String email = doc.getUser() != null ? doc.getUser().getEmail() : "";
        String name = doc.getUser() != null ? doc.getUser().getName() : "";

        if (email.equalsIgnoreCase("dr.sarah@hospital.com") || name.contains("Sarah")) {
            // Dr. Sarah: 03:00 PM -> 08:00 PM, 15-min interval, 20 patients maximum
            schedule.setStartTime(LocalTime.of(15, 0));
            schedule.setEndTime(LocalTime.of(20, 0));
            schedule.setMaxAppointments(20);
        } else if (email.equalsIgnoreCase("dr.robert@hospital.com") || name.contains("Robert")) {
            // Dr. Robert: 09:00 AM -> 01:00 PM, 15-min interval, 16 patients maximum
            schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(LocalTime.of(13, 0));
            schedule.setMaxAppointments(16);
        } else if (email.equalsIgnoreCase("dr.emily@hospital.com") || name.contains("Emily")) {
            // Dr. Emily: 10:00 AM -> 02:00 PM, 15-min interval, 16 patients maximum
            schedule.setStartTime(LocalTime.of(10, 0));
            schedule.setEndTime(LocalTime.of(14, 0));
            schedule.setMaxAppointments(16);
        } else {
            // Default shift: 09:00 AM -> 01:00 PM, 16 patients
            schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(LocalTime.of(13, 0));
            schedule.setMaxAppointments(16);
        }
        return schedule;
    }

    private void seedDoctorSchedule(Doctor doctor, LocalDate date) {
        if (!scheduleRepository.existsByDoctorIdAndAvailableDate(doctor.getId(), date)) {
            scheduleRepository.save(buildScheduleForDoctor(doctor, date));
        }
    }

    private void seedSamplePatient() {
        if (!userRepository.existsByEmail("patient@example.com")) {
            User user = new User();
            user.setName("John Doe");
            user.setEmail("patient@example.com");
            user.setPassword(passwordEncoder.encode("patient123"));
            user.setRole(Role.PATIENT);
            User savedUser = userRepository.save(user);

            Patient patient = new Patient();
            patient.setUser(savedUser);
            patient.setPhone("+1-555-0199");
            patient.setAge(32);
            patient.setGender(Gender.MALE);
            patientRepository.save(patient);

            log.info("Sample Patient John Doe seeded: patient@example.com / patient123");
        }
    }

    /**
     * UPDATE 1: Automatically ensures realistic schedules exist for every existing doctor
     * from TODAY through the NEXT 30 DAYS using doctor-specific working hours and capacity.
     * Idempotent check ensures no duplicates are created on restart.
     */
    private void ensureDoctorSchedulesForNext30Days() {
        List<Doctor> allDoctors = doctorRepository.findAll();
        LocalDate today = LocalDate.now();
        int createdCount = 0;

        for (Doctor doc : allDoctors) {
            for (int i = 0; i <= 30; i++) {
                LocalDate date = today.plusDays(i);
                if (!scheduleRepository.existsByDoctorIdAndAvailableDate(doc.getId(), date)) {
                    scheduleRepository.save(buildScheduleForDoctor(doc, date));
                    createdCount++;
                }
            }
        }

        if (createdCount > 0) {
            log.info("Ensured schedule availability for {} doctors (created {} missing shifts for next 30 days).",
                    allDoctors.size(), createdCount);
        }
    }

    /**
     * UPDATE 5: Provide simple database views doctor_details and patient_details
     * so that normalized names are directly visible when inspecting PostgreSQL.
     */
    private void createDatabaseViews() {
        try {
            jdbcTemplate.execute("""
                CREATE OR REPLACE VIEW doctor_details AS
                SELECT 
                    d.id AS doctor_id,
                    u.id AS user_id,
                    u.name AS doctor_name,
                    u.email AS email,
                    dept.id AS department_id,
                    dept.name AS department_name,
                    d.specialization AS specialization,
                    d.experience_years AS experience_years
                FROM doctors d
                JOIN users u ON d.user_id = u.id
                JOIN departments dept ON d.department_id = dept.id
            """);

            jdbcTemplate.execute("""
                CREATE OR REPLACE VIEW patient_details AS
                SELECT 
                    p.id AS patient_id,
                    u.id AS user_id,
                    u.name AS patient_name,
                    u.email AS email,
                    p.phone AS phone,
                    p.age AS age,
                    p.gender AS gender
                FROM patients p
                JOIN users u ON p.user_id = u.id
            """);

            log.info("Database views 'doctor_details' and 'patient_details' successfully verified/created in PostgreSQL.");
        } catch (Exception e) {
            log.warn("Could not create database views: {}", e.getMessage());
        }
    }
}
