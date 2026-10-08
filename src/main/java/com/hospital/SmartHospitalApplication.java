package com.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class SmartHospitalApplication {

    public static void main(String[] args) {
        // The hospital scheduling UI and seeded schedules are based on India Standard Time.
        // Render commonly runs containers in UTC, so set the JVM timezone explicitly to
        // keep booking cutoffs, schedule dates, queue dates, and timestamps consistent.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(SmartHospitalApplication.class, args);
    }
}
