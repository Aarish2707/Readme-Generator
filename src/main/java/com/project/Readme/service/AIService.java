package com.project.Readme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class AIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AIService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public String generateReadme(String repoAnalysis, String ownerName, String avatarUrl) {
        // Get current timestamp
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss");
        String currentTimestamp = now.format(formatter);
//        String prompt = "You are a professional README generator. " +
//                "Analyze the following repository details and create a clean, well-structured README.md file. " +
//                "The README should be professional, beginner-friendly, and formatted properly in Markdown. " +
//                "Follow this structure strictly:\n\n" +
//                "1. **Project Name**: Provide the name of the project.\n" +
//                "2. **Tech Stack**: List the languages, frameworks, and major libraries used.\n" +
//                "3. **Modules & Packages**: Summarize the key modules, packages, and their purposes.\n" +
//                "4. **Environment Variables**: Mention all required environment variables and provide clear steps on how to generate or configure them.\n" +
//                "5. **Setup Instructions**: Step-by-step guide on how to clone the repository and include the repo of the url yourself, install dependencies, configure environment variables, and run the project.\n" +
//                "6. **Directory Structure**: Provide a full directory/file tree of the repository in a properly formatted Markdown code block, making sure it is readable and clean.\n" +
//                "7. **Owner Details**: Include the repository owner's name"+(ownerName)+"in the footer.\n" +
//                "8. **Copyright & Timestamp**: End the README with a copyright statement and use this EXACT timestamp: " + currentTimestamp + "\n\n" +
//                "Make the README of at least 1400 words, clear, structured, and visually appealing. Make the formatting more appealing and with proper formatting for the directory structure" +
//                "you should also use the emoji's to make the readme more attractive at the necessary places." +
//                "also dont add any warning about the file structure's arrangement to make it feel real " +
//                "while generating the output give bullet points to the headings instead of the numberings  and also use proper heading tags for the above mentioned headings and enhance font sizes and also proper font styling to the generated text" +
//                "Do not omit any critical technical detail from the repository analysis.\n\n" +
//                repoAnalysis;
        String prompt = "You are a professional README generator. " +
                "IMPORTANT: Use ONLY the languages and technologies ACTUALLY DETECTED in the repository analysis. " +
                "DO NOT assume Python, pandas, numpy, or any technologies not explicitly mentioned.\n\n" +
                "Analyze the repository and create a README following this structure:\n\n" +
                "1. **Project Name**: Use the actual project name.\n" +
                "2. **Tech Stack**: List ONLY technologies found in 'DETECTED LANGUAGES' and 'PROJECT FILES DETECTED' sections.\n" +
                "3. **Modules and Packages**: Based on actual files found.\n" +
                "4. **Environment Variables**: From actual config files detected.\n" +
                "5. **Setup Instructions**: Match the detected project type (Java/Maven, React, etc.).\n" +
                "6. **Repository File Structure**: Use files from the analysis.\n" +
                "7. **Owner Details**: Include avatar (" + avatarUrl + ") and name (" + ownerName + ").\n" +
                "8. **Metadata**: Copyright and timestamp: " + currentTimestamp + "\n\n" +
                "CRITICAL: Only use technologies explicitly detected in the analysis below.\n\n" +
                repoAnalysis;



        try {
            // Build request body according to Gemini API specification
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", prompt);
            parts.add(part);
            
            content.set("parts", parts);
            contents.add(content);
            requestBody.set("contents", contents);
            
            // Add generation config to prevent issues
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 8192);
            requestBody.set("generationConfig", generationConfig);
            
            // Debug logging
            System.out.println("Request body: " + requestBody.toString());

            String response = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> new RuntimeException("API Error: " + errorBody)))
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.has("candidates") && jsonNode.get("candidates").size() > 0) {
                JsonNode candidate = jsonNode.get("candidates").get(0);
                if (candidate.has("content") && candidate.get("content").has("parts")) {
                    return candidate.get("content").get("parts").get(0).get("text").asText();
                }
            }
            
            return "⚠️ Unexpected response format: " + response;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error generating README: " + e.getMessage();
        }
    }
}
