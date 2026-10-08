package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hospital.entity.AppointmentStatus;
import com.hospital.entity.PriorityType;
import com.hospital.entity.QueueStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public class PatientQueueStatusResponse {

    private Long appointmentId;
    private String doctorName;
    private String departmentName;
    private PriorityType priorityType;
    private Integer priorityScore;
    private Integer queuePosition;
    private Integer patientsAhead;
    private AppointmentStatus appointmentStatus;
    private QueueStatus queueStatus;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime scheduleStartTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime scheduleEndTime;
    private String scheduleTime;
    private Integer patientsWaiting;
    private Integer queueSize;
    private Integer scheduleCapacity;
    private Long bookedCapacity;
    private String statusMessage;

    public PatientQueueStatusResponse() {
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
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

    public PriorityType getPriorityType() {
        return priorityType;
    }

    public void setPriorityType(PriorityType priorityType) {
        this.priorityType = priorityType;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Integer queuePosition) {
        this.queuePosition = queuePosition;
    }

    public Integer getPatientsAhead() {
        return patientsAhead;
    }

    public void setPatientsAhead(Integer patientsAhead) {
        this.patientsAhead = patientsAhead;
    }

    public AppointmentStatus getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(AppointmentStatus appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(QueueStatus queueStatus) {
        this.queueStatus = queueStatus;
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

    public LocalTime getScheduleStartTime() {
        return scheduleStartTime;
    }

    public void setScheduleStartTime(LocalTime scheduleStartTime) {
        this.scheduleStartTime = scheduleStartTime;
    }

    public LocalTime getScheduleEndTime() {
        return scheduleEndTime;
    }

    public void setScheduleEndTime(LocalTime scheduleEndTime) {
        this.scheduleEndTime = scheduleEndTime;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public Integer getPatientsWaiting() {
        return patientsWaiting;
    }

    public void setPatientsWaiting(Integer patientsWaiting) {
        this.patientsWaiting = patientsWaiting;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public Integer getScheduleCapacity() {
        return scheduleCapacity;
    }

    public void setScheduleCapacity(Integer scheduleCapacity) {
        this.scheduleCapacity = scheduleCapacity;
    }

    public Long getBookedCapacity() {
        return bookedCapacity;
    }

    public void setBookedCapacity(Long bookedCapacity) {
        this.bookedCapacity = bookedCapacity;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
