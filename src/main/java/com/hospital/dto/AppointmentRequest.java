package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hospital.entity.PriorityType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRequest {

    private Long scheduleId; // Optional DoctorSchedule ID

    private Long patientId; // Required if Admin creates appointment, otherwise determined from auth token

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date cannot be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    // Optional for patient: internally allocated across schedule shifts to prevent slot double booking
    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    @NotNull(message = "Priority type is required")
    private PriorityType priorityType;

    private String symptoms;

    public AppointmentRequest() {
    }

    public AppointmentRequest(Long patientId, Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime, PriorityType priorityType, String symptoms) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.priorityType = priorityType;
        this.symptoms = symptoms;
    }

    public AppointmentRequest(Long scheduleId, Long patientId, Long doctorId, LocalDate appointmentDate, PriorityType priorityType, String symptoms) {
        this.scheduleId = scheduleId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.priorityType = priorityType;
        this.symptoms = symptoms;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public PriorityType getPriorityType() {
        return priorityType;
    }

    public void setPriorityType(PriorityType priorityType) {
        this.priorityType = priorityType;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
}
