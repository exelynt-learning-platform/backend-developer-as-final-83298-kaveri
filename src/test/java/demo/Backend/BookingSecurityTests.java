package demo.Backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginReturnsJwtAndProtectedRoutesRejectMissingToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user","password":"User@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("user"));

        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void expiredTokenSaysTokenExpired() throws Exception {
        mockMvc.perform(get("/resources").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token expired"));
    }

    @Test
    void userCanReadResourcesButCannotManageThem() throws Exception {
        String user = token("user", "User@123");
        String admin = token("admin", "Admin@123");

        mockMvc.perform(get("/resources").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk());

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Blocked","type":"ROOM","price":10.00}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lab Projector","type":"EQUIPMENT","description":"HD","price":25.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Resource created successfully"))
                .andExpect(jsonPath("$.data.name").value("Lab Projector"))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void userSeesOnlyOwnReservationsAndAdminSeesAll() throws Exception {
        String user = token("user", "User@123");
        String other = registerAndToken("other-user-" + System.nanoTime());
        String admin = token("admin", "Admin@123");
        long resourceId = createResource(admin, "Private Room", "40.00");
        long otherResourceId = createResource(admin, "Other Room", "12345.67");

        long ownId = createReservation(user, resourceId, "2026-11-01T09:00:00", "2026-11-01T10:00:00", "40.00");
        long otherId = createReservation(other, otherResourceId, "2026-11-01T11:00:00", "2026-11-01T12:00:00", "12345.67");

        mockMvc.perform(get("/reservations/" + otherId).header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + ownId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id == " + otherId + ")]").doesNotExist());

        mockMvc.perform(get("/reservations/my").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your reservations fetched successfully"))
                .andExpect(jsonPath("$.data.content[?(@.id == " + ownId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id == " + otherId + ")]").doesNotExist());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + admin)
                        .param("status", "PENDING")
                        .param("minPrice", "12345.67")
                        .param("maxPrice", "12345.67")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "price")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + otherId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id == " + ownId + ")]").doesNotExist());

        mockMvc.perform(put("/reservations/" + ownId)
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/reservations/" + ownId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation updated successfully"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void overlappingReservationIsRejected() throws Exception {
        String user = token("user", "User@123");
        String admin = token("admin", "Admin@123");
        long resourceId = createResource(admin, "Overlap Room", "20.00");

        createReservation(user, resourceId, "2026-12-10T09:00:00", "2026-12-10T11:00:00", "20.00");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"startTime":"2026-12-10T10:00:00","endTime":"2026-12-10T12:00:00","price":20.00}
                                """.formatted(resourceId)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidReservationTimesAreRejected() throws Exception {
        String user = token("user", "User@123");
        String admin = token("admin", "Admin@123");
        long resourceId = createResource(admin, "Time Room", "15.00");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"startTime":"2026-12-01T12:00:00","endTime":"2026-12-01T10:00:00","price":15.00}
                                """.formatted(resourceId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"startTime":"2020-01-01T09:00:00","endTime":"2020-01-01T10:00:00","price":15.00}
                                """.formatted(resourceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservationPriceMustMatchResourcePrice() throws Exception {
        String user = token("user", "User@123");
        String admin = token("admin", "Admin@123");
        long resourceId = createResource(admin, "Price Room", "75.50");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"startTime":"2026-12-15T09:00:00","endTime":"2026-12-15T10:00:00","price":10.00}
                                """.formatted(resourceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Price must match the resource price of 75.50"));
    }

    @Test
    void reservationOwnerComesFromTokenNotRequestBody() throws Exception {
        String user = token("user", "User@123");
        String admin = token("admin", "Admin@123");
        long resourceId = createResource(admin, "Owner Room", "10.00");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"userId":999,"username":"admin","startTime":"2026-12-02T10:00:00","endTime":"2026-12-02T11:00:00","price":10.00}
                                """.formatted(resourceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("user"));
    }

    @Test
    void adminCanDeleteResourceWithoutReservations() throws Exception {
        String admin = token("admin", "Admin@123");
        long resourceId = createResource(admin, "Delete Me", "5.00");

        mockMvc.perform(put("/resources/" + resourceId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":700,"available":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource updated successfully"))
                .andExpect(jsonPath("$.data.price").value(700))
                .andExpect(jsonPath("$.data.name").value("Delete Me"));

        mockMvc.perform(delete("/resources/" + resourceId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource deleted successfully"));
    }

    private String registerAndToken(String username) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"User@123"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").doesNotExist());
        return token(username, "User@123");
    }

    private long createResource(String adminToken, String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"ROOM","price":%s}
                                """.formatted(name, price)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private long createReservation(String token, long resourceId, String start, String end, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"startTime":"%s","endTime":"%s","price":%s}
                                """.formatted(resourceId, start, end, price)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Reservation created successfully"))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get("id").asLong();
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key)
                .compact();
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
