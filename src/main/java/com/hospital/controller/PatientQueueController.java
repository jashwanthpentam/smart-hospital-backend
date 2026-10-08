package com.hospital.controller;

import com.hospital.dto.PatientQueueStatusResponse;
import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import com.hospital.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queue")
public class PatientQueueController {

    private final QueueService queueService;
    private final PatientRepository patientRepository;

    public PatientQueueController(QueueService queueService, PatientRepository patientRepository) {
        this.queueService = queueService;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientQueueStatusResponse> getMyQueueStatus(Authentication authentication) {
        Patient patient = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
        if (patient == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(queueService.getPatientQueueStatus(patient.getId()));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<PatientQueueStatusResponse> getPatientQueueStatus(@RequestParam Long patientId) {
        return ResponseEntity.ok(queueService.getPatientQueueStatus(patientId));
    }
}
