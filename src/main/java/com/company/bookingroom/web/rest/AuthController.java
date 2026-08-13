package com.company.bookingroom.web.rest;

import com.company.bookingroom.domain.User;
import com.company.bookingroom.repository.UserRepository;
import com.company.bookingroom.security.RoleMapping;
import com.company.bookingroom.service.UserService;
import com.company.bookingroom.service.dto.DepartmentDTO;
import com.company.bookingroom.web.rest.vm.AuthLoginResponse;
import com.company.bookingroom.web.rest.vm.AuthLoginVM;
import com.company.bookingroom.web.rest.vm.AuthSignupVM;
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
 * Spec-aligned auth: POST /api/auth/login, POST /api/auth/signup.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateController authenticateController;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(
        AuthenticateController authenticateController,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        UserRepository userRepository,
        UserService userService
    ) {
        this.authenticateController = authenticateController;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginVM loginVM) {
        return issueTokenResponse(loginVM.getEmail(), loginVM.getPassword(), HttpStatus.OK);
    }

    /**
     * Public sign-up — always creates ROLE_USER; ignores any client role field.
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthLoginResponse> signup(@Valid @RequestBody AuthSignupVM signupVM) {
        userService.registerPublicUser(
            signupVM.getEmail(),
            signupVM.getPassword(),
            signupVM.getFullName(),
            signupVM.getDepartmentId()
        );
        return issueTokenResponse(signupVM.getEmail(), signupVM.getPassword(), HttpStatus.CREATED);
    }

    private ResponseEntity<AuthLoginResponse> issueTokenResponse(String email, String password, HttpStatus status) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = authenticateController.createToken(authentication, false);

        User user = userRepository
            .findOneWithAuthoritiesByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String role = RoleMapping.toFrontendRole(user.getAuthorities());
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
        return new ResponseEntity<>(body, headers, status);
    }
}
