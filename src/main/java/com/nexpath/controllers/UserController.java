package com.nexpath.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    // 🔹 ONLY ADMIN
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Welcome Admin 🔥";
    }

    // 🔹 ONLY MENTOR
    @GetMapping("/mentor")
    @PreAuthorize("hasRole('MENTOR')")
    public String mentorAccess() {
        return "Welcome Mentor 🚀";
    }

    // 🔹 STUDENT + MENTOR + ADMIN
    @GetMapping("/common")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public String commonAccess() {
        return "Common access 👥";
    }

    // 🔹 ONLY AUTHENTICATED USER
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile() {
        return "User profile 🔐";
    }
}