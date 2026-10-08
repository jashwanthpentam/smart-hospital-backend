package com.hospital.service;

import com.hospital.dto.*;
import com.hospital.entity.*;
import com.hospital.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    public DashboardService(UserRepository userRepository,
                            DoctorRepository doctorRepository,
                            PatientRepository patientRepository,
                            DepartmentRepository departmentRepository,
                            AppointmentRepository appointmentRepository,
                            QueueEntryRepository queueEntryRepository,
                            QueueService queueService,
                            DoctorService doctorService,
                            AppointmentService appointmentService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.queueService = queueService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
    }

    public AdminDashboardResponse getAdminDashboard() {
        long totalPatients = patientRepository.count();
        long totalDoctors = doctorRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalAppointments = appointmentRepository.count();
        long waitingPatients = queueEntryRepository.countByQueueStatus(QueueStatus.WAITING);

        return new AdminDashboardResponse(
                totalPatients,
                totalDoctors,
                totalDepartments,
                totalAppointments,
                waitingPatients
        );
    }

    public DoctorDashboardResponse getDoctorDashboard(Long doctorId) {
        LocalDate today = LocalDate.now();
        List<Appointment> todayAppts = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, today);

        long todayCount = todayAppts.size();
        long waitingCount = todayAppts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.WAITING)
                .count();
        long completedCount = todayAppts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        QueueItemResponse activePatient = queueService.getActiveDoctorPatient(doctorId, today);
        List<QueueItemResponse> queue = queueService.getDoctorQueue(doctorId, today);

        DoctorDashboardResponse response = new DoctorDashboardResponse();
        response.setTodayAppointmentsCount(todayCount);
        response.setWaitingPatientsCount(waitingCount);
        response.setCompletedConsultationsCount(completedCount);
        response.setCurrentPatient(activePatient);
        response.setQueue(queue);

        return response;
    }

    public PatientDashboardResponse getPatientDashboard(Long patientId) {
        List<Appointment> appts = appointmentRepository.findByPatientId(patientId);

        Optional<Appointment> upcomingOpt = appts.stream()
                .filter(a -> (a.getStatus() == AppointmentStatus.WAITING || a.getStatus() == AppointmentStatus.IN_PROGRESS)
                          && !a.getAppointmentDate().isBefore(LocalDate.now()))
                .min(Comparator.comparing(Appointment::getAppointmentDate)
                        .thenComparing(Appointment::getAppointmentTime));

        AppointmentResponse upcoming = upcomingOpt.map(appointmentService::mapToResponse).orElse(null);
        PatientQueueStatusResponse queueStatus = queueService.getPatientQueueStatus(patientId);
        List<DoctorResponse> doctors = doctorService.getAllDoctors();

        PatientDashboardResponse response = new PatientDashboardResponse();
        response.setTotalAppointmentsCount(appts.size());
        response.setUpcomingAppointment(upcoming);
        response.setQueueStatus(queueStatus);
        response.setAvailableDoctors(doctors);

        return response;
    }
}
