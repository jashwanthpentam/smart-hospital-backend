package com.hospital.dto;

import java.util.List;

public class DoctorDashboardResponse {

    private long todayAppointmentsCount;
    private long waitingPatientsCount;
    private long completedConsultationsCount;
    private QueueItemResponse currentPatient;
    private List<QueueItemResponse> queue;

    public DoctorDashboardResponse() {
    }

    public long getTodayAppointmentsCount() {
        return todayAppointmentsCount;
    }

    public void setTodayAppointmentsCount(long todayAppointmentsCount) {
        this.todayAppointmentsCount = todayAppointmentsCount;
    }

    public long getWaitingPatientsCount() {
        return waitingPatientsCount;
    }

    public void setWaitingPatientsCount(long waitingPatientsCount) {
        this.waitingPatientsCount = waitingPatientsCount;
    }

    public long getCompletedConsultationsCount() {
        return completedConsultationsCount;
    }

    public void setCompletedConsultationsCount(long completedConsultationsCount) {
        this.completedConsultationsCount = completedConsultationsCount;
    }

    public QueueItemResponse getCurrentPatient() {
        return currentPatient;
    }

    public void setCurrentPatient(QueueItemResponse currentPatient) {
        this.currentPatient = currentPatient;
    }

    public List<QueueItemResponse> getQueue() {
        return queue;
    }

    public void setQueue(List<QueueItemResponse> queue) {
        this.queue = queue;
    }
}
