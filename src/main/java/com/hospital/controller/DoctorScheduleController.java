package com.hospital.controller;

import com.hospital.dto.ScheduleRequest;
import com.hospital.dto.ScheduleResponse;
import com.hospital.entity.Doctor;
import com.hospital.repository.DoctorRepository;
import com.hospital.service.DoctorScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class DoctorScheduleController {

    private final DoctorScheduleService scheduleService;
    private final DoctorRepository doctorRepository;

    public DoctorScheduleController(DoctorScheduleService scheduleService, DoctorRepository doctorRepository) {
        this.scheduleService = scheduleService;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules(
            @RequestParam(required = false) Long doctorId) {
        if (doctorId != null) {
            return ResponseEntity.ok(scheduleService.getSchedulesByDoctor(doctorId));
        }
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {
        Long doctorId = null;
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            Doctor doctor = doctorRepository.findByUserEmail(authentication.getName()).orElse(null);
            if (doctor != null) {
                doctorId = doctor.getId();
            }
        }
        ScheduleResponse created = scheduleService.createSchedule(request, doctorId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {
        Long authenticatedDoctorId = resolveAuthenticatedDoctorId(authentication);
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request, authenticatedDoctorId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long id,
            Authentication authentication) {
        Long authenticatedDoctorId = resolveAuthenticatedDoctorId(authentication);
        scheduleService.deleteSchedule(id, authenticatedDoctorId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveAuthenticatedDoctorId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        boolean isDoctor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (!isDoctor) {
            return null;
        }
        Doctor doctor = doctorRepository.findByUserEmail(authentication.getName()).orElse(null);
        if (doctor == null) {
            throw new com.hospital.exception.BadRequestException("Doctor could not be identified");
        }
        return doctor.getId();
    }
}
