package demo.Backend.Controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class ApiDocsController {

    @GetMapping("/api-docs")
    public void redirect(HttpServletResponse response) throws IOException {
        response.sendRedirect("/api-docs/");
    }

    @GetMapping("/api-docs/")
    public String index() {
        return "forward:/api-docs/index.html";
    }
}
