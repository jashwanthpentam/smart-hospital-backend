package com.hospital.service;

import com.hospital.dto.ScheduleRequest;
import com.hospital.dto.ScheduleResponse;
import com.hospital.entity.AppointmentStatus;
import com.hospital.entity.Doctor;
import com.hospital.entity.DoctorSchedule;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ConflictException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleService {

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorScheduleService(DoctorScheduleRepository scheduleRepository,
                                 DoctorRepository doctorRepository,
                                 AppointmentRepository appointmentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getSchedulesByDoctor(Long doctorId) {
        return scheduleRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ScheduleResponse getScheduleById(Long id) {
        DoctorSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        return mapToResponse(schedule);
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request, Long authenticatedDoctorId) {
        Long doctorId = request.getDoctorId() != null ? request.getDoctorId() : authenticatedDoctorId;
        if (doctorId == null) {
            throw new BadRequestException("Doctor ID must be specified");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        if (request.getMaxAppointments() <= 0) {
            throw new BadRequestException("Maximum appointments capacity must be greater than 0");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));

        if (scheduleRepository.existsByDoctorIdAndAvailableDate(doctorId, request.getAvailableDate())) {
            throw new ConflictException("Schedule already exists for this doctor on " + request.getAvailableDate());
        }

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setAvailableDate(request.getAvailableDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMaxAppointments(request.getMaxAppointments());

        DoctorSchedule saved = scheduleRepository.save(schedule);
        return mapToResponse(saved);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long id, ScheduleRequest request) {
        DoctorSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        if (request.getMaxAppointments() <= 0) {
            throw new BadRequestException("Maximum appointments capacity must be greater than 0");
        }

        if (!schedule.getAvailableDate().equals(request.getAvailableDate()) &&
                scheduleRepository.existsByDoctorIdAndAvailableDate(schedule.getDoctor().getId(), request.getAvailableDate())) {
            throw new ConflictException("Schedule already exists for this doctor on " + request.getAvailableDate());
        }

        schedule.setAvailableDate(request.getAvailableDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMaxAppointments(request.getMaxAppointments());

        DoctorSchedule updated = scheduleRepository.save(schedule);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Schedule not found with id: " + id);
        }
        scheduleRepository.deleteById(id);
    }

    public ScheduleResponse mapToResponse(DoctorSchedule schedule) {
        long booked = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusNot(
                schedule.getDoctor().getId(),
                schedule.getAvailableDate(),
                AppointmentStatus.CANCELLED
        );

        ScheduleResponse res = new ScheduleResponse(
                schedule.getId(),
                schedule.getDoctor().getId(),
                schedule.getDoctor().getUser().getName(),
                schedule.getDoctor().getDepartment().getName(),
                schedule.getAvailableDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getMaxAppointments(),
                booked
        );

        java.time.LocalDateTime scheduleStart = java.time.LocalDateTime.of(schedule.getAvailableDate(), schedule.getStartTime());
        java.time.LocalDateTime cutoff = scheduleStart.minusHours(6);
        boolean isNotCutoff = java.time.LocalDateTime.now().isBefore(cutoff);
        boolean hasCapacity = booked < schedule.getMaxAppointments();
        res.setIsBookingOpen(isNotCutoff && hasCapacity);

        return res;
    }
}
