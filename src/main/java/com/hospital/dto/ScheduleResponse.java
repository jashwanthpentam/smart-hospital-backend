package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate availableDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private Integer maxAppointments;
    private Long bookedCount;
    private Integer remainingCapacity;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime bookingCutoffTime;
    private Boolean isBookingOpen;
    private Integer consultationDurationMinutes = 15;

    public ScheduleResponse() {
    }

    public ScheduleResponse(Long id, Long doctorId, String doctorName, String departmentName, LocalDate availableDate, LocalTime startTime, LocalTime endTime, Integer maxAppointments, Long bookedCount) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.departmentName = departmentName;
        this.availableDate = availableDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxAppointments = maxAppointments;
        this.bookedCount = bookedCount;
        this.remainingCapacity = (maxAppointments != null && bookedCount != null) ? Math.max(0, maxAppointments - bookedCount.intValue()) : 0;
        if (startTime != null) {
            this.bookingCutoffTime = startTime.minusHours(6);
        }
        this.consultationDurationMinutes = 15;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LocalDate getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDate availableDate) {
        this.availableDate = availableDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getMaxAppointments() {
        return maxAppointments;
    }

    public void setMaxAppointments(Integer maxAppointments) {
        this.maxAppointments = maxAppointments;
    }

    public Long getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(Long bookedCount) {
        this.bookedCount = bookedCount;
    }

    public Integer getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Integer remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public LocalTime getBookingCutoffTime() {
        return bookingCutoffTime;
    }

    public void setBookingCutoffTime(LocalTime bookingCutoffTime) {
        this.bookingCutoffTime = bookingCutoffTime;
    }

    public Boolean getIsBookingOpen() {
        return isBookingOpen;
    }

    public void setIsBookingOpen(Boolean isBookingOpen) {
        this.isBookingOpen = isBookingOpen;
    }

    public Integer getConsultationDurationMinutes() {
        return consultationDurationMinutes;
    }

    public void setConsultationDurationMinutes(Integer consultationDurationMinutes) {
        this.consultationDurationMinutes = consultationDurationMinutes;
    }
}
