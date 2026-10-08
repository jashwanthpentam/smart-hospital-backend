package com.hospital.controller;

import com.hospital.dto.QueueItemResponse;
import com.hospital.entity.Doctor;
import com.hospital.exception.BadRequestException;
import com.hospital.repository.DoctorRepository;
import com.hospital.service.QueueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorQueueController {

    private final QueueService queueService;
    private final DoctorRepository doctorRepository;

    public DoctorQueueController(QueueService queueService, DoctorRepository doctorRepository) {
        this.queueService = queueService;
        this.doctorRepository = doctorRepository;
    }

    private Long resolveDoctorId(Authentication authentication, Long queryDoctorId) {
        if (queryDoctorId != null) {
            return queryDoctorId;
        }
        if (authentication != null) {
            Doctor doctor = doctorRepository.findByUserEmail(authentication.getName()).orElse(null);
            if (doctor != null) {
                return doctor.getId();
            }
        }
        throw new BadRequestException("Doctor could not be identified");
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<QueueItemResponse>> getDoctorQueue(
            Authentication authentication,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long resolvedDoctorId = resolveDoctorId(authentication, doctorId);
        return ResponseEntity.ok(queueService.getDoctorQueue(resolvedDoctorId, date));
    }

    @GetMapping("/queue/next")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueItemResponse> peekNextPatient(
            Authentication authentication,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long resolvedDoctorId = resolveDoctorId(authentication, doctorId);
        return ResponseEntity.ok(queueService.peekNextPatient(resolvedDoctorId, date));
    }

    @GetMapping("/queue/active")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueItemResponse> getActivePatient(
            Authentication authentication,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long resolvedDoctorId = resolveDoctorId(authentication, doctorId);
        return ResponseEntity.ok(queueService.getActiveDoctorPatient(resolvedDoctorId, date));
    }

    @PutMapping("/queue/call-next")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueItemResponse> callNextPatient(
            Authentication authentication,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long resolvedDoctorId = resolveDoctorId(authentication, doctorId);
        return ResponseEntity.ok(queueService.callNextPatient(resolvedDoctorId, date));
    }

    @PutMapping("/appointments/{id}/start")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueItemResponse> startConsultation(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) Long doctorId) {
        Long resolvedDoctorId = resolveDoctorId(authentication, doctorId);
        return ResponseEntity.ok(queueService.startConsultation(id, resolvedDoctorId));
    }

    @PutMapping("/appointments/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueItemResponse> completeConsultation(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) Long doctorId) {
        Long resolvedDoctorId = resolveDoctorId(authentication, doctorId);
        return ResponseEntity.ok(queueService.completeConsultation(id, resolvedDoctorId));
    }
}
