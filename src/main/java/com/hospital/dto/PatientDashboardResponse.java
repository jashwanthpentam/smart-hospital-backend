package com.hospital.dto;

import java.util.List;

public class PatientDashboardResponse {

    private long totalAppointmentsCount;
    private AppointmentResponse upcomingAppointment;
    private PatientQueueStatusResponse queueStatus;
    private List<DoctorResponse> availableDoctors;

    public PatientDashboardResponse() {
    }

    public long getTotalAppointmentsCount() {
        return totalAppointmentsCount;
    }

    public void setTotalAppointmentsCount(long totalAppointmentsCount) {
        this.totalAppointmentsCount = totalAppointmentsCount;
    }

    public AppointmentResponse getUpcomingAppointment() {
        return upcomingAppointment;
    }

    public void setUpcomingAppointment(AppointmentResponse upcomingAppointment) {
        this.upcomingAppointment = upcomingAppointment;
    }

    public PatientQueueStatusResponse getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(PatientQueueStatusResponse queueStatus) {
        this.queueStatus = queueStatus;
    }

    public List<DoctorResponse> getAvailableDoctors() {
        return availableDoctors;
    }

    public void setAvailableDoctors(List<DoctorResponse> availableDoctors) {
        this.availableDoctors = availableDoctors;
    }
}
