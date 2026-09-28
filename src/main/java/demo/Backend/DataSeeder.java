package demo.Backend;

import demo.Backend.Entity.User;
import demo.Backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("admin", "Admin@123", "ADMIN");
        seedUser("user", "User@123", "USER");
    }

    private void seedUser(String username, String rawPassword, String role) {
        if (userRepo.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepo.save(user);
    }
}
