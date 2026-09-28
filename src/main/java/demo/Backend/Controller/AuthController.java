package demo.Backend.Controller;

import demo.Backend.DTO.LoginRequest;
import demo.Backend.DTO.LoginResponse;
import demo.Backend.DTO.RegisterRequest;
import demo.Backend.DTO.RegisterResponse;
import demo.Backend.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response=authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest Request) {
        LoginResponse response=authService.login(Request);
        return ResponseEntity.ok(response);
    }
}
