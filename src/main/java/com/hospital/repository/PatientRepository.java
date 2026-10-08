package com.hospital.repository;

import com.hospital.entity.Patient;
import com.hospital.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUser(User user);
    Optional<Patient> findByUserId(Long userId);
    Optional<Patient> findByUserEmail(String email);
    Page<Patient> findAll(Pageable pageable);
}
