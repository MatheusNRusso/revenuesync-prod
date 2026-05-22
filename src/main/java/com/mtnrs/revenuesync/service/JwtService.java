package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @Value("${api.security.jwt.secret}") private String secret;
    @Value("${api.security.jwt.expiration-ms}") private long expirationMs;
    private SecretKey getSigningKey(){ return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    public String generateToken(User user){
        return Jwts.builder().subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+expirationMs))
                .signWith(getSigningKey()).compact();
    }
    public String extractEmail(String token){ return extractClaims(token).getSubject(); }
    public Long extractUserId(String token){ return extractClaims(token).get("userId", Long.class); }
    public String extractRole(String token){ return extractClaims(token).get("role", String.class); }
    public boolean isTokenValid(String token, User user){ return user.getEmail().equals(extractEmail(token)) && !isTokenExpired(token); }
    private boolean isTokenExpired(String token){ return extractClaims(token).getExpiration().before(new Date()); }
    private Claims extractClaims(String token){ return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload(); }
}