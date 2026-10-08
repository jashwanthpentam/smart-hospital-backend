package com.hospital.controller;

import com.hospital.dto.AdminDashboardResponse;
import com.hospital.dto.DoctorDashboardResponse;
import com.hospital.dto.PatientDashboardResponse;
import com.hospital.entity.Doctor;
import com.hospital.entity.Patient;
import com.hospital.exception.BadRequestException;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public DashboardController(DashboardService dashboardService,
                               DoctorRepository doctorRepository,
                               PatientRepository patientRepository) {
        this.dashboardService = dashboardService;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<DoctorDashboardResponse> getDoctorDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long doctorId) {
        Long resolvedDoctorId = doctorId;
        if (resolvedDoctorId == null && authentication != null) {
            Doctor doc = doctorRepository.findByUserEmail(authentication.getName()).orElse(null);
            if (doc != null) {
                resolvedDoctorId = doc.getId();
            }
        }
        if (resolvedDoctorId == null) {
            throw new BadRequestException("Doctor ID could not be identified");
        }
        return ResponseEntity.ok(dashboardService.getDoctorDashboard(resolvedDoctorId));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public ResponseEntity<PatientDashboardResponse> getPatientDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long patientId) {
        Long resolvedPatientId = patientId;
        if (resolvedPatientId == null && authentication != null) {
            Patient p = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
            if (p != null) {
                resolvedPatientId = p.getId();
            }
        }
        if (resolvedPatientId == null) {
            throw new BadRequestException("Patient ID could not be identified");
        }
        return ResponseEntity.ok(dashboardService.getPatientDashboard(resolvedPatientId));
    }
}
