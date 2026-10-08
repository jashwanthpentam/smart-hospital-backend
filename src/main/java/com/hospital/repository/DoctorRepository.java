package com.hospital.repository;

import com.hospital.entity.Doctor;
import com.hospital.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUser(User user);
    Optional<Doctor> findByUserId(Long userId);
    Optional<Doctor> findByUserEmail(String email);
    List<Doctor> findByDepartmentId(Long departmentId);
    Page<Doctor> findAll(Pageable pageable);
}
