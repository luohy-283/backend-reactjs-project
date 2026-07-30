package com.company.bookingroom.web.rest.vm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login credentials matching the exercise API contract (email + password).
 */
public class AuthLoginVM {

    @NotBlank
    @Email
    @Size(min = 5, max = 254)
    private String email;

    @NotBlank
    @Size(min = 4, max = 100)
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
