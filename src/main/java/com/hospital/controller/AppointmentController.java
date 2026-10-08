package com.hospital.controller;

import com.hospital.dto.AppointmentRequest;
import com.hospital.dto.AppointmentResponse;
import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import com.hospital.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    public AppointmentController(AppointmentService appointmentService, PatientRepository patientRepository) {
        this.appointmentService = appointmentService;
        this.patientRepository = patientRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        if (patientId != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
        }
        if (doctorId != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
        }
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Page<AppointmentResponse>> getAppointmentsPaged(
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.getAppointmentsPaged(pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication authentication) {
        Patient patient = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
        if (patient == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patient.getId()));
    }

    @GetMapping("/my/page")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Page<AppointmentResponse>> getMyAppointmentsPaged(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Patient patient = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
        if (patient == null) {
            return ResponseEntity.ok(Page.empty());
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatientPaged(patient.getId(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody AppointmentRequest request,
            Authentication authentication) {
        Long authenticatedPatientId = null;
        boolean isPatient = false;
        if (authentication != null) {
            isPatient = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
            Patient patient = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
            if (patient != null) {
                authenticatedPatientId = patient.getId();
            }
        }

        // Patient bookings always use the patient represented by the JWT.
        // Admin/doctor bookings may explicitly supply patientId.
        if (isPatient) {
            request.setPatientId(authenticatedPatientId);
        }

        AppointmentResponse created = appointmentService.bookAppointment(request, authenticatedPatientId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            Authentication authentication) {
        Long authenticatedPatientId = null;
        boolean isAdminOrDoctor = false;
        if (authentication != null) {
            isAdminOrDoctor = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DOCTOR"));
            Patient patient = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
            if (patient != null) {
                authenticatedPatientId = patient.getId();
            }
        }
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, authenticatedPatientId, isAdminOrDoctor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
