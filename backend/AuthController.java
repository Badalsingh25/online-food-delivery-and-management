package com.hungerexpress.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @jakarta.validation.Valid SignupRequest req){
        return ResponseEntity.ok(auth.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @jakarta.validation.Valid AuthRequest req){
        return ResponseEntity.ok(auth.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(@RequestHeader(name = "Authorization", required = false) String authorization){
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authorization.substring(7);
        return ResponseEntity.ok(auth.me(token));
    }

    public record RefreshRequest(String refreshToken){}

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req){
        return ResponseEntity.ok(auth.refresh(req.refreshToken()));
    }
}
