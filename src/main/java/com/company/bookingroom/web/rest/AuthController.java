package com.company.bookingroom.web.rest;

import com.company.bookingroom.domain.User;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.AuthoritiesConstants;
import com.company.bookingroom.service.dto.DepartmentDTO;
import com.company.bookingroom.web.rest.vm.AuthLoginResponse;
import com.company.bookingroom.web.rest.vm.AuthLoginVM;
import com.company.bookingroom.web.rest.vm.AuthUserVM;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spec-aligned auth endpoint: POST /api/auth/login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateController authenticateController;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;

    public AuthController(
        AuthenticateController authenticateController,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        UserRepository userRepository
    ) {
        this.authenticateController = authenticateController;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginVM loginVM) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(loginVM.getEmail(), loginVM.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = authenticateController.createToken(authentication, false);

        User user = userRepository
            .findOneWithAuthoritiesByEmailIgnoreCase(loginVM.getEmail())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String role = toFrontendRole(user);
        DepartmentDTO departmentDTO = null;
        if (user.getDepartment() != null) {
            departmentDTO = new DepartmentDTO();
            departmentDTO.setId(user.getDepartment().getId());
            departmentDTO.setCode(user.getDepartment().getCode());
            departmentDTO.setName(user.getDepartment().getName());
        }
        AuthUserVM userVM = new AuthUserVM(user.getId(), user.getEmail(), user.getFullName(), role, departmentDTO);
        AuthLoginResponse body = new AuthLoginResponse(jwt, userVM);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private static String toFrontendRole(User user) {
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> AuthoritiesConstants.ADMIN.equals(a.getName()));
        return isAdmin ? "ADMIN" : "USER";
    }
}
