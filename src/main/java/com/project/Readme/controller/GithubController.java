package com.project.Readme.controller;

import com.project.Readme.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GithubController {

    @Autowired
    private GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @GetMapping("/repos")
    public ResponseEntity<?> getOwnedRepos(@RequestHeader("Authorization") String authHeader) {
        try {
            String jwtToken = extractToken(authHeader);
            return ResponseEntity.ok(githubService.getOwnedRepositories(jwtToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify-ownership")
    public ResponseEntity<?> verifyOwnership(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String ownerName
    ) {
        try {
            String jwtToken = extractToken(authHeader);
            var repos = githubService.getOwnedRepositories(jwtToken);
            
            boolean isOwned = repos.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .anyMatch(repo -> {
                        Object ownerObj = ((Map<?, ?>) repo).get("owner");
                        if (ownerObj instanceof Map<?, ?> ownerMap) {
                            Object loginObj = ownerMap.get("login");
                            return loginObj instanceof String login && ownerName.equalsIgnoreCase(login);
                        }
                        return false;
                    });

            return ResponseEntity.ok(Map.of("owned", isOwned));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        return authHeader.substring(7);
    }
}