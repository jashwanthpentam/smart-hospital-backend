package com.hospital.repository;

import com.hospital.entity.Doctor;
import com.hospital.entity.DoctorSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctor(Doctor doctor);
    List<DoctorSchedule> findByDoctorId(Long doctorId);
    Optional<DoctorSchedule> findByDoctorIdAndAvailableDate(Long doctorId, LocalDate availableDate);
    boolean existsByDoctorIdAndAvailableDate(Long doctorId, LocalDate availableDate);
    Page<DoctorSchedule> findAll(Pageable pageable);
}
