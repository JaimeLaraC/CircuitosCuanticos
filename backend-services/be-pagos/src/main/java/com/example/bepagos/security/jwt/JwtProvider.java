package com.example.bepagos.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
                       @Value("${jwt.expires-in:1h}") String expiresIn) { // Default expires-in if not set
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationInMs = parseExpiresInToMs(expiresIn);
    }

    private long parseExpiresInToMs(String expiresIn) {
        if (expiresIn == null || expiresIn.length() < 2) throw new IllegalArgumentException("Invalid JWT expiration format: " + expiresIn);
        String unit = expiresIn.substring(expiresIn.length() - 1);
        long value = Long.parseLong(expiresIn.substring(0, expiresIn.length() - 1));
        switch (unit.toLowerCase()) {
            case "s": return value * 1000;
            case "m": return value * 60 * 1000;
            case "h": return value * 60 * 60 * 1000;
            default: throw new IllegalArgumentException("Invalid JWT expiration unit: " + unit);
        }
    }

    // Added for testing convenience - mirrors BE Usuarios/Circuitos test adaptation
    public String generateToken(String subject, Long userId, String email) {
         Map<String, Object> claims = new HashMap<>();
         claims.put("userId", userId);
         claims.put("email", email);

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

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
         final Claims claims = extractAllClaims(token);
         Object userIdObj = claims.get("userId");
         if (userIdObj instanceof Integer) {
             return ((Integer) userIdObj).longValue();
         } else if (userIdObj instanceof Long) {
             return (Long) userIdObj;
         }
         return null;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
