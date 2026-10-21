package com.model;

import java.io.Serializable;

public final class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String rollNo;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final String collegeName;
    private final String passwordHash;
    private final String role;
    private final String department;
    private final int year;
    private final String gender;
    private final String address;

    public User(
            String rollNo,
            String fullName,
            String email,
            String phoneNumber,
            String collegeName,
            String passwordHash,
            String role,
            String department,
            int year,
            String gender,
            String address) {

        this.rollNo = rollNo;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.collegeName = collegeName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.department = department;
        this.year = year;
        this.gender = gender;
        this.address = address;
    }

    public String rollNo() {
        return rollNo;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String collegeName() {
        return collegeName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String role() {
        return role;
    }

    public String department() {
        return department;
    }

    public int year() {
        return year;
    }

    public String gender() {
        return gender;
    }

    public String address() {
        return address;
    }
}
