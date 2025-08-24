package com.project.Readme.controller;

import com.project.Readme.model.User;
import com.project.Readme.repository.UserRepository;
import com.project.Readme.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8080")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Value("${github.client.id:your_github_client_id}")
    private String clientId;

    @Value("${github.client.secret:your_github_client_secret}")
    private String clientSecret;

    @Value("${BASE_URL:http://localhost:8080/}")
    private String baseUrl;

    @GetMapping("/github")
    public RedirectView githubAuth() {
        String redirectUri = baseUrl + "api/auth/callback";
        String scope = "user:email,repo";

        String githubAuthUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scope +
                "&response_type=code";

        return new RedirectView(githubAuthUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code) {
        System.out.println("Received callback with code: " + code);
        try {
            // Exchange code for access token
            String accessToken = getAccessToken(code);
            System.out.println("Got access token: " + accessToken);

            // Get user info from GitHub
            JsonNode userInfo = getUserInfo(accessToken);
            System.out.println("Got user info: " + userInfo.toString());

            String login = userInfo.get("login").asText();
            String name = userInfo.has("name") && !userInfo.get("name").isNull() ? userInfo.get("name").asText() : login;
            String avatarUrl = userInfo.get("avatar_url").asText();
            String githubId = userInfo.get("id").asText();
            String email = userInfo.has("email") && !userInfo.get("email").isNull() ? userInfo.get("email").asText() : null;

            // Save user to database
            System.out.println("=== Saving User to Database ===");
            User user = userRepository.findByGithubId(githubId).orElse(new User());
            user.setGithubId(githubId);
            user.setUsername(login);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setAccessToken(accessToken);

            User savedUser = userRepository.save(user);
            System.out.println("User saved with ID: " + savedUser.getId());

            // Generate JWT token
            String jwtToken = jwtService.generateToken(githubId);

            String redirectUrl = baseUrl + "dashboard?login=" + login +
                    "&name=" + name + "&avatar=" + avatarUrl + "&githubId=" + githubId + "&token=" + jwtToken;
            System.out.println("Redirecting to: " + redirectUrl);

            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in callback: " + e.getMessage());
            return new RedirectView(baseUrl + "login?error=" + e.getMessage());
        }
    }

    private String getAccessToken(String code) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        String tokenUrl = "https://github.com/login/oauth/access_token";
        String requestBody = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code=" + code;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.getBody());

        return jsonNode.get("access_token").asText();
    }

    private JsonNode getUserInfo(String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(response.getBody());
    }
}