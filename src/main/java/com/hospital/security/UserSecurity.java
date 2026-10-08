package com.hospital.security;

import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    private final PatientRepository patientRepository;

    public UserSecurity(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public boolean isCurrentPatient(Authentication authentication, Long patientId) {
        if (authentication == null || patientId == null) {
            return false;
        }
        Patient patient = patientRepository.findByUserEmail(authentication.getName()).orElse(null);
        return patient != null && patient.getId().equals(patientId);
    }
}
