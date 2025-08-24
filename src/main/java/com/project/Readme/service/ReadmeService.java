package com.project.Readme.service;

import com.project.Readme.model.ReadmeFile;
import com.project.Readme.repository.ReadmeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReadmeService {

    @Autowired
    private GithubService gitHubService;
    @Autowired
    private ReadmeRepository readmeRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AIService aiService;

    public String generateReadme(String jwtToken, String ownerName, String repoName, String description) {
        String githubId = jwtService.extractGithubId(jwtToken.replace("Bearer ", ""));

        // Verify repository ownership
        boolean ownsRepo = gitHubService.getOwnedRepositories(jwtToken).stream()
                .filter(Objects::nonNull)
                .map(Map.class::cast)
                .anyMatch(repo -> {
                    Object ownerObj = ((Map<?, ?>) repo).get("owner");
                    if (ownerObj instanceof Map<?, ?> ownerMap) {
                        Object loginObj = ownerMap.get("login");
                        return loginObj instanceof String login && login.equalsIgnoreCase(ownerName);
                    }
                    return false;
                });

        if (!ownsRepo) {
            throw new RuntimeException("You do not own this repository!");
        }

        // Get comprehensive repository context for AI
        Map<String, Object> repoDetails = gitHubService.getRepositoryDetails(jwtToken, ownerName, repoName);
        String repoStructure = gitHubService.getRepositoryFiles(jwtToken, ownerName, repoName);
        String projectAnalysis = gitHubService.analyzeProjectStructure(jwtToken, ownerName, repoName);

        // Build comprehensive context for AI
        String projectContext = buildProjectContext(repoDetails, repoStructure, projectAnalysis, description);

        // Extract avatar URL from repository details
        String avatarUrl = repoDetails != null && repoDetails.get("owner") instanceof Map ?
                (String) ((Map<?, ?>) repoDetails.get("owner")).get("avatar_url") : "";

        // Generate README using AI
        String content = aiService.generateReadme(projectContext, ownerName, avatarUrl);

        // Save to database
        ReadmeFile readmeFile = new ReadmeFile(null, githubId, repoName, ownerName, content, LocalDateTime.now());
        readmeRepository.save(readmeFile);

        return content;
    }

    private String buildProjectContext(Map<String, Object> repoDetails, String repoStructure, String projectAnalysis, String description) {
        StringBuilder context = new StringBuilder();

        context.append("=== REPOSITORY INFORMATION ===\n");
        if (repoDetails != null) {
            context.append("Repository: ").append(repoDetails.get("name")).append("\n");
            context.append("Primary Language: ").append(repoDetails.get("language")).append("\n");
            context.append("Description: ").append(description != null ? description : repoDetails.get("description")).append("\n");
            context.append("Stars: ").append(repoDetails.get("stargazers_count")).append("\n");
            context.append("Forks: ").append(repoDetails.get("forks_count")).append("\n");
            context.append("Default Branch: ").append(repoDetails.get("default_branch")).append("\n\n");
        }

        context.append("=== PROJECT STRUCTURE ===\n");
        context.append(repoStructure).append("\n");

        context.append("=== CONFIGURATION FILES ANALYSIS ===\n");
        context.append(projectAnalysis);

        context.append("\n=== INSTRUCTIONS ===\n");
        context.append("Based on the above analysis, generate a comprehensive README that includes:\n");
        context.append("1. Detected tech stack and frameworks\n");
        context.append("2. All dependencies from package files\n");
        context.append("3. Required environment variables\n");
        context.append("4. Step-by-step setup instructions\n");
        context.append("5. Usage examples based on the project type\n");

        return context.toString();
    }

    public ReadmeFile generateAndSaveReadme(String repoUrl, String githubId) {
        // Extract owner and repo name from URL
        String[] parts = extractOwnerAndRepo(repoUrl);
        String ownerName = parts[0];
        String repoName = parts[1];

        // Verify repository ownership
        if (!isRepositoryOwnedByUser(githubId, ownerName, repoName)) {
            throw new RuntimeException("Cannot generate the repository as you don't have the access to the repository.");
        }

        // Fetch repository data from GitHub API with user authentication
        String repoAnalysis = fetchRepositoryAnalysisWithAuth(ownerName, repoName, githubId);

        // Generate README using AI
        String avatarUrl = "https://github.com/" + ownerName + ".png";
        String content = aiService.generateReadme(repoAnalysis, ownerName, avatarUrl);

        // Save to database with user's GitHub ID
        ReadmeFile readmeFile = new ReadmeFile(null, githubId, repoName, ownerName, content, LocalDateTime.now());
        return readmeRepository.save(readmeFile);
    }



    private String fetchAuthenticatedGitHubData(String url, String accessToken) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "README-Generator");
            
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class).getBody();
        } catch (Exception e) {
            System.err.println("Error fetching GitHub data from " + url + ": " + e.getMessage());
            return "{}";
        }
    }
    
    private String fetchConfigFiles(String owner, String repo, String accessToken) {
        StringBuilder configAnalysis = new StringBuilder();
        String[] configFiles = {"package.json", "pom.xml", "build.gradle", "requirements.txt", 
                               "Dockerfile", "docker-compose.yml", ".env.example", "application.yml", "application.properties"};
        
        for (String fileName : configFiles) {
            try {
                String fileContent = fetchFileContent(owner, repo, fileName, accessToken);
                if (fileContent != null && !fileContent.equals("{}")) {
                    configAnalysis.append("\n=== ").append(fileName).append(" ===\n");
                    // Limit content to prevent overwhelming the AI
                    configAnalysis.append(fileContent.length() > 2000 ? 
                        fileContent.substring(0, 2000) + "..." : fileContent);
                    configAnalysis.append("\n");
                }
            } catch (Exception e) {
                // File doesn't exist, continue
            }
        }
        
        return configAnalysis.toString();
    }
    
    private String fetchFileContent(String owner, String repo, String fileName, String accessToken) {
        try {
            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + fileName;
            String response = fetchAuthenticatedGitHubData(url, accessToken);
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode fileNode = mapper.readTree(response);
            
            if (fileNode.has("content") && "file".equals(fileNode.path("type").asText())) {
                String encodedContent = fileNode.path("content").asText();
                return new String(java.util.Base64.getDecoder().decode(encodedContent.replaceAll("\\s", "")));
            }
        } catch (Exception e) {
            // File not found or error
        }
        return null;
    }
    
    private String fetchRepositoryAnalysisWithAuth(String owner, String repo, String githubId) {
        try {
            // Get user's access token for authenticated requests
            com.project.Readme.model.User user = userRepository.findByGithubId(githubId).orElse(null);
            String accessToken = user != null ? user.getAccessToken() : null;
            
            // Fetch repository details with authentication
            String repoData = fetchAuthenticatedGitHubData("https://api.github.com/repos/" + owner + "/" + repo, accessToken);
            
            // Fetch repository languages
            String languagesData = fetchAuthenticatedGitHubData("https://api.github.com/repos/" + owner + "/" + repo + "/languages", accessToken);
            
            // Fetch repository contents
            String contentsData = fetchAuthenticatedGitHubData("https://api.github.com/repos/" + owner + "/" + repo + "/contents", accessToken);
            
            // Fetch specific config files for better analysis
            String configAnalysis = fetchConfigFiles(owner, repo, accessToken);
            
            String analysis = buildEnhancedAnalysis(repoData, languagesData, contentsData, configAnalysis, owner, repo);
            
            // Debug logging
            System.out.println("=== REPOSITORY ANALYSIS FOR AI ===");
            System.out.println(analysis);
            System.out.println("=== END ANALYSIS ===");
            
            return analysis;
        } catch (Exception e) {
            System.err.println("Error fetching repository analysis: " + e.getMessage());
            e.printStackTrace();
            return "Repository: " + repo + "\nOwner: " + owner + "\nError: Unable to fetch detailed analysis - " + e.getMessage();
        }
    }

    private String buildEnhancedAnalysis(String repoData, String languagesData, String contentsData, String configAnalysis, String owner, String repo) {
        StringBuilder analysis = new StringBuilder();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            // Parse repository data
            com.fasterxml.jackson.databind.JsonNode repoNode = mapper.readTree(repoData);
            analysis.append("=== REPOSITORY INFORMATION ===\n");
            analysis.append("Name: ").append(repoNode.path("name").asText()).append("\n");
            analysis.append("Description: ").append(repoNode.path("description").asText()).append("\n");
            analysis.append("Primary Language: ").append(repoNode.path("language").asText()).append("\n");
            analysis.append("Stars: ").append(repoNode.path("stargazers_count").asInt()).append("\n");
            analysis.append("Forks: ").append(repoNode.path("forks_count").asInt()).append("\n\n");

            // Parse languages data
            com.fasterxml.jackson.databind.JsonNode languagesNode = mapper.readTree(languagesData);
            analysis.append("=== DETECTED LANGUAGES ===\n");
            languagesNode.fieldNames().forEachRemaining(lang ->
                    analysis.append(lang).append(": ").append(languagesNode.path(lang).asLong()).append(" bytes\n")
            );
            analysis.append("\n");

            // Parse contents to detect frameworks
            com.fasterxml.jackson.databind.JsonNode contentsNode = mapper.readTree(contentsData);
            analysis.append("=== PROJECT FILES DETECTED ===\n");
            if (contentsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode file : contentsNode) {
                    String fileName = file.path("name").asText();
                    analysis.append("- ").append(fileName).append("\n");

                    // Detect framework based on files
                    if (fileName.equals("package.json")) {
                        analysis.append("  → Node.js/JavaScript project detected\n");
                    } else if (fileName.equals("pom.xml")) {
                        analysis.append("  → Maven/Java project detected\n");
                    } else if (fileName.equals("build.gradle")) {
                        analysis.append("  → Gradle/Java project detected\n");
                    } else if (fileName.equals("requirements.txt") || fileName.equals("setup.py")) {
                        analysis.append("  → Python project detected\n");
                    } else if (fileName.equals("Cargo.toml")) {
                        analysis.append("  → Rust project detected\n");
                    } else if (fileName.equals("go.mod")) {
                        analysis.append("  → Go project detected\n");
                    }
                }
            }

            // Add configuration files analysis
            analysis.append("\n=== CONFIGURATION FILES ANALYSIS ===\n");
            analysis.append(configAnalysis);
            
        } catch (Exception e) {
            analysis.append("Repository: ").append(repo).append("\nOwner: ").append(owner);
            analysis.append("\nError parsing repository data: ").append(e.getMessage());
        }

        return analysis.toString();
    }

    public List<ReadmeFile> getUserReadmesByGithubId(String githubId) {
        return readmeRepository.findByGithubId(githubId);
    }

    private String[] extractOwnerAndRepo(String repoUrl) {
        // Extract from GitHub URL: https://github.com/owner/repo
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        if (parts.length >= 2) {
            return new String[]{parts[0], parts[1]};
        }
        throw new IllegalArgumentException("Invalid GitHub repository URL");
    }

    public List<ReadmeFile> getUserReadmes(String jwtToken) {
        String githubId = jwtService.extractGithubId(jwtToken.replace("Bearer ", ""));
        return readmeRepository.findByGithubId(githubId);
    }

    private boolean isRepositoryOwnedByUser(String githubId, String ownerName, String repoName) {
        try {
            // Get user's access token
            com.project.Readme.model.User user = userRepository.findByGithubId(githubId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String accessToken = user.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return false;
            }

            // Check if user owns the repository by fetching their repositories
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            // Get user's owned repositories
            String url = "https://api.github.com/user/repos?type=owner&per_page=100";
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);

            // Parse response to check if the repository exists in user's owned repos
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode repos = mapper.readTree(response.getBody());

            if (repos.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode repo : repos) {
                    String repoOwner = repo.path("owner").path("login").asText();
                    String repoNameFromApi = repo.path("name").asText();

                    if (ownerName.equalsIgnoreCase(repoOwner) && repoName.equalsIgnoreCase(repoNameFromApi)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private com.project.Readme.repository.UserRepository userRepository;
}