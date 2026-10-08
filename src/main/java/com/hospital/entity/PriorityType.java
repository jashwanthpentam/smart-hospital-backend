package com.hospital.entity;

public enum PriorityType {
    NORMAL(20),
    URGENT(50),
    EMERGENCY(100);

    private final int defaultScore;

    PriorityType(int defaultScore) {
        this.defaultScore = defaultScore;
    }

    public int getDefaultScore() {
        return defaultScore;
    }
}
