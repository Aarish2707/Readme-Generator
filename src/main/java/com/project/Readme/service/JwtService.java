package com.project.Readme.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    @Value("${JWT_SECRET:ORlzsrUcwmqVMTgQm77ck2IHSCB4MZqzRtF6qqZ2lxydnzGai0BD1Zz7Z9JvmhAa92mHTZ3GSjXuMBDGHzjInQ==}")
    private String secretKey;

    @Value("${JWT_EXPIRATION:86400000}")
    private long expiration;

    public String generateToken(String githubId) {
        return Jwts.builder()
                .setSubject(githubId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractGithubId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public boolean isTokenValid(String token, String githubId) {
        String extractedId = extractGithubId(token);
        return (extractedId.equals(githubId) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

}