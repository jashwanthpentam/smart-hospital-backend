package com.hospital.dto;

public class AdminDashboardResponse {

    private long totalPatients;
    private long totalDoctors;
    private long totalDepartments;
    private long totalAppointments;
    private long waitingPatients;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(long totalPatients, long totalDoctors, long totalDepartments, long totalAppointments, long waitingPatients) {
        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.totalDepartments = totalDepartments;
        this.totalAppointments = totalAppointments;
        this.waitingPatients = waitingPatients;
    }

    public long getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(long totalPatients) {
        this.totalPatients = totalPatients;
    }

    public long getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(long totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public long getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(long totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getWaitingPatients() {
        return waitingPatients;
    }

    public void setWaitingPatients(long waitingPatients) {
        this.waitingPatients = waitingPatients;
    }
}
