package com.example.beusuarios.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.beusuarios.model.User; // Assuming User model exists

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationInMs;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expires-in}") String expiresIn) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // Parse expiresIn: "1h" -> ms, "60m" -> ms, "3600s" -> ms
        this.jwtExpirationInMs = parseExpiresInToMs(expiresIn);
    }

    private long parseExpiresInToMs(String expiresIn) {
        String unit = expiresIn.substring(expiresIn.length() - 1);
        long value = Long.parseLong(expiresIn.substring(0, expiresIn.length() - 1));
        switch (unit.toLowerCase()) {
            case "s": return value * 1000;
            case "m": return value * 60 * 1000;
            case "h": return value * 60 * 60 * 1000;
            default: throw new IllegalArgumentException("Invalid JWT expiration unit: " + unit);
        }
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail()); // Storing decrypted email in token
        // Add other claims as needed, e.g., roles or authorities

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail()) // Or user.getId().toString()
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(secretKey, SignatureAlgorithm.HS512) // Using HS512
                .compact();
    }

    public String generateToken(String subject, Long userId, String email) {
         Map<String, Object> claims = new HashMap<>();
         claims.put("userId", userId);
         claims.put("email", email); // Storing decrypted email in token

         return Jwts.builder()
                 .setClaims(claims)
                 .setSubject(subject)
                 .setIssuedAt(new Date(System.currentTimeMillis()))
                 .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                 .signWith(secretKey, SignatureAlgorithm.HS512)
                 .compact();
     }


    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) { // Or extractSubject
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
         final Claims claims = extractAllClaims(token);
         return claims.get("userId", Long.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, User user) { // Or validate against just username/subject
        final String username = extractUsername(token);
        // Compare with user's current details if needed, or just check expiration and signature
        return (username.equals(user.getEmail()) && !isTokenExpired(token));
    }

     public Boolean validateToken(String token) {
         try {
             Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
             return !isTokenExpired(token);
         } catch (Exception e) { // Catches signature, malformed, expired etc.
             return false;
         }
     }
}
