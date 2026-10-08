-- ============================================================
-- Smart Hospital Appointment & Priority Queue Management System
-- Database Schema (MySQL 8.x)
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_hospital;
USE smart_hospital;

-- Disable foreign key checks for clean teardown/rebuild
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS queue_entries;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS doctor_schedules;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('PATIENT', 'DOCTOR', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Departments Table
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- 3. Patients Table (1:1 with users)
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    age INT NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Doctors Table (1:1 with users, N:1 with departments)
CREATE TABLE doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    experience_years INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_doctor_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
);

-- 5. Doctor Schedules Table (N:1 with doctors)
CREATE TABLE doctor_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    available_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_appointments INT NOT NULL DEFAULT 10,
    CONSTRAINT fk_sched_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT uq_doctor_date UNIQUE (doctor_id, available_date)
);

-- 6. Appointments Table
CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    priority_type ENUM('NORMAL', 'URGENT', 'EMERGENCY') NOT NULL DEFAULT 'NORMAL',
    symptoms TEXT,
    status ENUM('BOOKED', 'WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'WAITING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- 7. Queue Entries Table (1:1 with appointments)
CREATE TABLE queue_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    priority_score INT NOT NULL DEFAULT 20, -- EMERGENCY=100, URGENT=50, NORMAL=20
    queue_status ENUM('WAITING', 'CALLED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED') NOT NULL DEFAULT 'WAITING',
    arrival_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_queue_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_appt_doc_date ON appointments(doctor_id, appointment_date);
CREATE INDEX idx_queue_status ON queue_entries(queue_status);
CREATE INDEX idx_queue_score_arr ON queue_entries(priority_score DESC, arrival_time ASC);

-- 8. Views for Easy Inspection of Normalized Names (UPDATE 5)
CREATE OR REPLACE VIEW doctor_details AS
SELECT 
    d.id AS doctor_id,
    u.id AS user_id,
    u.name AS doctor_name,
    u.email AS email,
    dept.id AS department_id,
    dept.name AS department_name,
    d.specialization AS specialization,
    d.experience_years AS experience_years
FROM doctors d
JOIN users u ON d.user_id = u.id
JOIN departments dept ON d.department_id = dept.id;

CREATE OR REPLACE VIEW patient_details AS
SELECT 
    p.id AS patient_id,
    u.id AS user_id,
    u.name AS patient_name,
    u.email AS email,
    p.phone AS phone,
    p.age AS age,
    p.gender AS gender
FROM patients p
JOIN users u ON p.user_id = u.id;

