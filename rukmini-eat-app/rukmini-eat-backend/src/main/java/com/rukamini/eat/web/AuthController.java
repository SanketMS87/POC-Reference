package com.rukamini.eat.web;

import com.rukamini.eat.dto.Dto.AuthResponse;
import com.rukamini.eat.dto.Dto.LoginRequest;
import com.rukamini.eat.dto.Dto.RegisterRequest;
import com.rukamini.eat.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@SecurityRequirements
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a customer account and return a JWT")
    @ApiResponse(responseCode = "429", description = "Too many attempts from this address; see Retry-After",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "201", description = "Account created; token and user returned")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Email is already registered",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and return a JWT")
    @ApiResponse(responseCode = "429", description = "Too many attempts from this address; see Retry-After",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "200", description = "Login succeeded; token and user returned")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
