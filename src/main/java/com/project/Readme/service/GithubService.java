package com.project.Readme.service;

import com.project.Readme.model.User;
import com.project.Readme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GithubService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    public List<Map<String, Object>> getOwnedRepositories(String jwtToken) {
        if (!StringUtils.hasText(jwtToken)) {
            throw new IllegalArgumentException("JWT token is required");
        }

        try {
            // Extract GitHub ID from JWT token
            String githubId = jwtService.extractGithubId(jwtToken.replace("Bearer ", ""));

            // Get user from database using GitHub ID
            User user = userRepository.findByGithubId(githubId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get GitHub access token from database
            String accessToken = user.getAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                throw new RuntimeException("GitHub access token not found for user");
            }

            // Use the access token to call GitHub API
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/user/repos?type=owner&sort=updated";

            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch repositories: " + e.getMessage(), e);
        }
    }

    public String getGithubAccessToken(String jwtToken) {
        String githubId = jwtService.extractGithubId(jwtToken.replace("Bearer ", ""));
        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getAccessToken();
    }

    public Map<String, Object> getRepositoryDetails(String jwtToken, String ownerName, String repoName) {
        String accessToken = getGithubAccessToken(jwtToken);
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = String.format("https://api.github.com/repos/%s/%s", ownerName, repoName);
        
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public String analyzeProjectStructure(String jwtToken, String ownerName, String repoName) {
        StringBuilder analysis = new StringBuilder();
        
        // Check for common config files
        String[] configFiles = {"package.json", "pom.xml", "requirements.txt", "Gemfile", 
                               "composer.json", "go.mod", "Cargo.toml", ".env.example", 
                               "docker-compose.yml", "Dockerfile"};
        
        for (String file : configFiles) {
            String content = getFileContent(jwtToken, ownerName, repoName, file);
            if (content != null) {
                analysis.append("\n=== ").append(file).append(" ===\n");
                analysis.append(content.length() > 1000 ? content.substring(0, 1000) + "..." : content);
                analysis.append("\n");
            }
        }
        
        return analysis.toString();
    }

    public String getRepositoryFiles(String jwtToken, String ownerName, String repoName) {
        String accessToken = getGithubAccessToken(jwtToken);
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Get repository contents
        String url = String.format("https://api.github.com/repos/%s/%s/contents", ownerName, repoName);
        
        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map[].class);
            Map[] files = response.getBody();
            
            StringBuilder context = new StringBuilder();
            context.append("Repository Structure:\n");
            
            if (files != null) {
                for (Map<String, Object> file : files) {
                    String fileName = (String) file.get("name");
                    String fileType = (String) file.get("type");
                    context.append("- ").append(fileName).append(" (").append(fileType).append(")\n");
                }
            }
            
            return context.toString();
        } catch (Exception e) {
            return "Unable to fetch repository structure";
        }
    }

    public String getFileContent(String jwtToken, String ownerName, String repoName, String filePath) {
        String accessToken = getGithubAccessToken(jwtToken);
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", ownerName, repoName, filePath);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> fileData = response.getBody();
            
            if (fileData != null && "file".equals(fileData.get("type"))) {
                String content = (String) fileData.get("content");
                // Decode base64 content
                return new String(java.util.Base64.getDecoder().decode(content));
            }
        } catch (Exception e) {
            // File not found or error
        }
        
        return null;
    }
}