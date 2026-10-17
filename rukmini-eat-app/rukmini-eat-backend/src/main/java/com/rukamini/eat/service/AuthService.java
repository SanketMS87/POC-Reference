package com.rukamini.eat.service;

import com.rukamini.eat.domain.Role;
import com.rukamini.eat.domain.User;
import com.rukamini.eat.dto.Dto.AuthResponse;
import com.rukamini.eat.dto.Dto.LoginRequest;
import com.rukamini.eat.dto.Dto.RegisterRequest;
import com.rukamini.eat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = users.save(new User(req.name().trim(), email, encoder.encode(req.password()), Role.CUSTOMER));
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase()).orElse(null);
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return respond(user);
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(jwt.generate(user), Mapper.user(user));
    }
}
