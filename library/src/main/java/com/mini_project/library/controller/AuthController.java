package com.mini_project.library.controller;

import com.mini_project.library.dto.LoginRequest;
import com.mini_project.library.dto.SessionResponse;
import com.mini_project.library.dto.SignupRequest;
import com.mini_project.library.security.AuthUser;
import com.mini_project.library.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse signup(@RequestBody @Valid SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public SessionResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public SessionResponse me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user);
    }
}
