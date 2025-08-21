//package com.project.Readme.controller;
//
//import com.project.Readme.model.User;
//import com.project.Readme.repository.UserRepository;
//import com.project.Readme.service.JwtService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.view.RedirectView;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.http.*;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//@RestController
//@RequestMapping("/api/auth")
//@CrossOrigin(origins = {"http://localhost:3000","http://localhost:5173"}, allowCredentials = "true")
//public class AuthController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private JwtService jwtService;
//
//    @Value("${github.client.id:your_github_client_id}")
//    private String clientId;
//
//    @Value("${github.client.secret:your_github_client_secret}")
//    private String clientSecret;
//
//    @Value("${BASE_URL:http://localhost:8080}")
//    private String baseUrl;
//
//    @GetMapping("/github")
//    public RedirectView githubAuth() {
//        String redirectUri = baseUrl + "/api/auth/callback";
//        String scope = "user:email,repo";
//
//        String githubAuthUrl = "https://github.com/login/oauth/authorize" +
//                "?client_id=" + clientId +
//                "&redirect_uri=" + redirectUri +
//                "&scope=" + scope +
//                "&response_type=code";
//
//        return new RedirectView(githubAuthUrl);
//    }
//
//    @GetMapping("/callback")
//    public RedirectView callback(@RequestParam String code) {
//        System.out.println("Received callback with code: " + code);
//        try {
//            // Exchange code for access token
//            String accessToken = getAccessToken(code);
//            System.out.println("Got access token: " + accessToken);
//
//            // Get user info from GitHub
//            JsonNode userInfo = getUserInfo(accessToken);
//            System.out.println("Got user info: " + userInfo.toString());
//
//            String login = userInfo.get("login").asText();
//            String name = userInfo.has("name") && !userInfo.get("name").isNull() ? userInfo.get("name").asText() : login;
//            String avatarUrl = userInfo.get("avatar_url").asText();
//            String githubId = userInfo.get("id").asText();
//            String email = userInfo.has("email") && !userInfo.get("email").isNull() ? userInfo.get("email").asText() : null;
//
//            // Save user to database
//            System.out.println("=== Saving User to Database ===");
//            User user = userRepository.findByGithubId(githubId).orElse(new User());
//            user.setGithubId(githubId);
//            user.setUsername(login);
//            user.setEmail(email);
//            user.setAvatarUrl(avatarUrl);
//            user.setAccessToken(accessToken);
//
//            User savedUser = userRepository.save(user);
//            System.out.println("User saved with ID: " + savedUser.getId());
//
//            // Generate JWT token
//            String jwtToken = jwtService.generateToken(githubId);
//
//            // Use configured baseUrl for dashboard redirect
//            String dashboardUrl = baseUrl;
//            String redirectUrl = dashboardUrl + "/dashboard?login=" + login +
//                    "&name=" + name + "&avatar=" + avatarUrl + "&githubId=" + githubId + "&token=" + jwtToken;
//            System.out.println("Redirecting to: " + redirectUrl);
//
//            return new RedirectView(redirectUrl);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("Error in callback: " + e.getMessage());
//            return new RedirectView(baseUrl + "/login?error=" + e.getMessage());
//        }
//    }
//
//    private String getAccessToken(String code) throws Exception {
//        RestTemplate restTemplate = new RestTemplate();
//
//        String tokenUrl = "https://github.com/login/oauth/access_token";
//        String requestBody = "client_id=" + clientId +
//                "&client_secret=" + clientSecret +
//                "&code=" + code;
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.set("Accept", "application/json");
//
//        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
//        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode jsonNode = mapper.readTree(response.getBody());
//
//        return jsonNode.get("access_token").asText();
//    }
//
//    private JsonNode getUserInfo(String accessToken) throws Exception {
//        RestTemplate restTemplate = new RestTemplate();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + accessToken);
//        headers.set("Accept", "application/vnd.github.v3+json");
//
//        HttpEntity<String> request = new HttpEntity<>(headers);
//        ResponseEntity<String> response = restTemplate.exchange(
//                "https://api.github.com/user", HttpMethod.GET, request, String.class);
//
//        ObjectMapper mapper = new ObjectMapper();
//        return mapper.readTree(response.getBody());
//    }
//}

package com.project.Readme.controller;

import com.project.Readme.model.User;
import com.project.Readme.repository.UserRepository;
import com.project.Readme.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:5173"}, allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Value("${GITHUB_CLIENT_ID:}")
    private String clientId;

    @Value("${GITHUB_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/github")
    public RedirectView githubAuth(HttpServletResponse response) {
        // create state and set cookie
        String state = UUID.randomUUID().toString();

        Cookie stateCookie = new Cookie("oauth_state", state);
        stateCookie.setHttpOnly(true);
        stateCookie.setSecure(false); // set true when behind HTTPS
        stateCookie.setPath("/");
        stateCookie.setMaxAge(300); // 5 minutes
        response.addCookie(stateCookie);

        String redirectUri = backendUrl + "/api/auth/callback";
        String scope = "user:email,repo"; // reduce if you don’t need 'repo'

        String githubAuthUrl =
                "https://github.com/login/oauth/authorize" +
                        "?client_id=" + url(clientId) +
                        "&redirect_uri=" + url(redirectUri) +
                        "&scope=" + url(scope) +
                        "&state=" + url(state) +
                        "&response_type=code";

        return new RedirectView(githubAuthUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code,
                                 @RequestParam(required = false) String state,
                                 HttpServletRequest request) {
        try {
            // validate state from cookie
            String stateFromCookie = readCookie(request, "oauth_state");
            if (stateFromCookie == null || !stateFromCookie.equals(state)) {
                return new RedirectView(frontendUrl + "/login?error=invalid_state");
            }

            String redirectUri = backendUrl + "/api/auth/callback";
            String accessToken = getAccessToken(code, redirectUri);

            // fetch user info
            JsonNode userInfo = getUserInfo(accessToken);

            String login = userInfo.get("login").asText();
            String name = userInfo.hasNonNull("name") ? userInfo.get("name").asText() : login;
            String avatarUrl = userInfo.get("avatar_url").asText();
            String githubId = userInfo.get("id").asText();

            // email can be null if private – try /user/emails
            String email = null;
            if (userInfo.hasNonNull("email")) {
                email = userInfo.get("email").asText();
            } else {
                email = fetchPrimaryEmail(accessToken);
            }

            // Save/update user
            User user = userRepository.findByGithubId(githubId).orElse(new User());
            user.setGithubId(githubId);
            user.setUsername(login);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setAccessToken(accessToken);
            userRepository.save(user);

            // Issue JWT
            String jwtToken = jwtService.generateToken(githubId);

            // Redirect to frontend dashboard with params
            String redirectUrl = String.format(
                    "%s/dashboard?login=%s&name=%s&avatar=%s&githubId=%s&token=%s",
                    frontendUrl,
                    url(login),
                    url(name),
                    url(avatarUrl),
                    url(githubId),
                    url(jwtToken)
            );
            return new RedirectView(redirectUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return new RedirectView(frontendUrl + "/login?error=" + url(e.getMessage()));
        }
    }

    private String getAccessToken(String code, String redirectUri) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        String tokenUrl = "https://github.com/login/oauth/access_token";
        String requestBody = "client_id=" + url(clientId) +
                "&client_secret=" + url(clientSecret) +
                "&code=" + url(code) +
                "&redirect_uri=" + url(redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "README-Generator");

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

        JsonNode jsonNode = mapper.readTree(response.getBody());
        if (jsonNode.hasNonNull("error")) {
            throw new RuntimeException("GitHub token error: " + jsonNode.get("error").asText());
        }
        return jsonNode.get("access_token").asText();
    }

    private JsonNode getUserInfo(String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "README-Generator");

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, request, String.class);

        return mapper.readTree(response.getBody());
    }

    private String fetchPrimaryEmail(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "README-Generator");

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.github.com/user/emails", HttpMethod.GET, request, String.class);

            JsonNode arr = mapper.readTree(response.getBody());
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    boolean primary = n.path("primary").asBoolean(false);
                    boolean verified = n.path("verified").asBoolean(false);
                    if (primary && verified) {
                        return n.path("email").asText(null);
                    }
                }
                // fallback to first email
                if (!arr.isEmpty()) return arr.get(0).path("email").asText(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String url(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
