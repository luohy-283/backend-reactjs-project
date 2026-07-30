package com.company.bookingroom.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * Self-service account update — department cannot be set here.
 */
public class AccountUpdateDTO implements Serializable {

    @Size(max = 100)
    private String fullName;

    @Email
    @Size(min = 5, max = 254)
    private String email;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
