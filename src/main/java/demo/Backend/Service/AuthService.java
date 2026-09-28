package demo.Backend.Service;

import demo.Backend.DTO.LoginRequest;
import demo.Backend.DTO.LoginResponse;
import demo.Backend.DTO.RegisterRequest;
import demo.Backend.DTO.RegisterResponse;
import demo.Backend.Entity.User;
import demo.Backend.Exception.ApiException;
import demo.Backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {
        requireCredentials(request.getUsername(), request.getPassword());
        String username = request.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        User user=new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        User savedUser=userRepository.save(user);
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername());
    }
    public LoginResponse login(LoginRequest request) {
        requireCredentials(request.getUsername(), request.getPassword());
        User user = userRepository
                .findByUsername(request.getUsername().trim())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        boolean passwordMatches = passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword());

        if (!passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(token, user.getUsername());
    }

    private void requireCredentials(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
    }
}