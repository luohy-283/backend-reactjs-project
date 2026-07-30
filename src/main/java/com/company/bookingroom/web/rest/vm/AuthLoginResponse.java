package com.company.bookingroom.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Login response matching the frontend contract: { token, user }.
 */
public class AuthLoginResponse {

    private String token;
    private AuthUserVM user;

    public AuthLoginResponse(String token, AuthUserVM user) {
        this.token = token;
        this.user = user;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AuthUserVM getUser() {
        return user;
    }

    public void setUser(AuthUserVM user) {
        this.user = user;
    }
}
