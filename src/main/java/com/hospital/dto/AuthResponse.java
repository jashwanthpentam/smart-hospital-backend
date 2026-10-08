package com.hospital.dto;

import com.hospital.entity.Role;

public class AuthResponse {

    private String token;
    private Long id;
    private Long specificId; // patientId or doctorId if applicable
    private String name;
    private String email;
    private Role role;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, Long specificId, String name, String email, Role role) {
        this.token = token;
        this.id = id;
        this.specificId = specificId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpecificId() {
        return specificId;
    }

    public void setSpecificId(Long specificId) {
        this.specificId = specificId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
