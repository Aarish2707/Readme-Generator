package com.project.Readme.controller;

import com.project.Readme.model.ReadmeFile;
import com.project.Readme.service.ReadmeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/readme")
@CrossOrigin(origins = "http://localhost:3000")
public class ReadmeController {

    @Autowired
    private ReadmeService readmeService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateReadme(
            @RequestHeader("X-GitHub-ID") String githubId,
            @RequestBody Map<String, String> payload
    ) {
        try {
            String repoUrl = payload.get("repoUrl");
            if (repoUrl == null || repoUrl.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Repository URL is required"));
            }
            
            ReadmeFile savedReadme = readmeService.generateAndSaveReadme(repoUrl, githubId);
            return ResponseEntity.ok(Map.of(
                "id", savedReadme.getId(),
                "content", savedReadme.getContent(),
                "repositoryName", savedReadme.getRepoName(),
                "createdAt", savedReadme.getCreatedAt()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            
            // Check if it's an access/ownership error
            if (e.getMessage() != null && e.getMessage().contains("don't have the access to the repository")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            }
            
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReadmeFile>> getUserReadmes(
            @RequestHeader("X-GitHub-ID") String githubId
    ) {
        try {
            List<ReadmeFile> readmes = readmeService.getUserReadmesByGithubId(githubId);
            return ResponseEntity.ok(readmes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of());
        }
    }
}