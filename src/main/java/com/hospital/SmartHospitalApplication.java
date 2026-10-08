package com.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartHospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartHospitalApplication.class, args);
    }
}
