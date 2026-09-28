package demo.Backend.Service;

import demo.Backend.Entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final String secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        String role = user.getRole() == null ? "USER" : user.getRole().toUpperCase();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public enum TokenStatus {
        VALID,
        EXPIRED,
        INVALID
    }

    public TokenStatus status(String token) {
        if (token == null || token.isBlank()) {
            return TokenStatus.INVALID;
        }
        try {
            parse(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException exception) {
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException exception) {
            return TokenStatus.INVALID;
        }
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = parse(token).get("role");
        return role == null ? "USER" : role.toString().toUpperCase();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
