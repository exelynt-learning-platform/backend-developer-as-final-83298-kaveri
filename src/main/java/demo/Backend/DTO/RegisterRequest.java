package demo.Backend.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class RegisterRequest {
    @JsonAlias({"UserName", "userName"})
    private String username;

    @JsonAlias("Password")
    private String password;
}
