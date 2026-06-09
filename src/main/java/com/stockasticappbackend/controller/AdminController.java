package com.stockasticappbackend.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for administrative dashboard endpoints.
 * Provides basic admin-only endpoints accessible at /admin.
 * All endpoints require ADMIN role authentication.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * Retrieves the admin dashboard welcome message.
     *
     * @return A welcome message for authenticated administrators.
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "Welcome Admin";
    }
}
