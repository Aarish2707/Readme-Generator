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
                "Create a comprehensive README.md file based STRICTLY on the repository analysis provided below. " +
                "\n\nCRITICAL RULES:\n" +
                "- ONLY use technologies, languages, and frameworks explicitly mentioned in the analysis\n" +
                "- If no specific technologies are detected, state 'Technology stack to be determined based on project analysis'\n" +
                "- DO NOT assume or add Python, pandas, numpy, or any other technologies not found in the analysis\n" +
                "- If the analysis shows Java/Maven, focus on Java ecosystem\n" +
                "- If the analysis shows Node.js/React, focus on JavaScript ecosystem\n" +
                "- Base ALL content on the actual files and languages detected\n" +
                "- Generate AT LEAST 1100 words of content with detailed explanations\n" +
                "- Use proper Markdown styling with enhanced headings and formatting\n\n" +
                "FORMATTING REQUIREMENTS:\n" +
                "- Use # for main title with center alignment: <h1 align=\"center\">Project Name</h1>\n" +
                "- Use ## for section headings with proper styling\n" +
                "- Use ### for subsections where appropriate\n" +
                "- Make profile image circular: <img src=\"" + avatarUrl + "\" width='50' height='50' style='border-radius: 50%;'>\n" +
                "- Use bold text, code blocks, and proper spacing for better readability\n" +
                "- Add horizontal rules (---) between major sections\n\n" +
                "Structure the README as follows:\n\n" +
                "<h1 align=\"center\">🎯 [Project Name]</h1>\n" +
                "<p align=\"center\">Extract and use the actual project name and description from repository information</p>\n\n" +
                "---\n\n" +
                "## 🚀 **Tech Stack**\n" +
                "List ONLY the technologies found in the 'DETECTED LANGUAGES' and 'PROJECT FILES DETECTED' sections. " +
                "Provide detailed explanations of each technology's role. " +
                "If Java and pom.xml are detected, mention Java, Maven, Spring Boot (if applicable). " +
                "If package.json is detected, mention Node.js and frameworks found in dependencies.\n\n" +
                "---\n\n" +
                "## 📦 **Modules and Packages**\n" +
                "Based on the project structure and detected files, provide detailed descriptions of main modules, " +
                "their purposes, dependencies, and how they interact. Include subsections for different module categories.\n\n" +
                "---\n\n" +
                "## ⚙️ **Environment Variables**\n" +
                "List environment variables based on configuration files found (.env, application.yml, etc.). " +
                "Provide detailed explanations of each variable's purpose and how to obtain/generate values.\n\n" +
                "---\n\n" +
                "## 🛠️ **Setup Instructions**\n" +
                "Provide comprehensive setup steps matching the detected project type (Maven for Java, npm for Node.js, etc.). " +
                "Include prerequisites, installation steps, configuration, and running instructions. " +
                "Add troubleshooting tips and common issues.\n\n" +
                "---\n\n" +
                "## 📁 **Repository Structure**\n" +
                "Create a detailed tree structure using the files listed in the analysis. " +
                "Explain the purpose of key directories and files.\n\n" +
                "---\n\n" +
                "## 🚀 **Usage Examples**\n" +
                "Provide practical usage examples based on the project type and detected features.\n\n" +
                "---\n\n" +
                "## 👤 **Owner Details**\n" +
                "<div align=\"center\">\n" +
                "<img src=\"" + avatarUrl + "\" width='80' height='80' style='border-radius: 50%; border: 3px solid #0366d6;'>\n" +
                "<h3>" + ownerName + "</h3>\n" +
                "</div>\n\n" +
                "---\n\n" +
                "## 📄 **Metadata**\n" +
                "**Copyright © 2024 " + ownerName + "**\n\n" +
                "**Generated:** " + currentTimestamp + "\n\n" +
                "<p align=\"center\"><em>This README was generated by AI Readme Generator</em></p>\n\n" +
                "Repository Analysis:\n" + repoAnalysis;



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
            System.out.println("=== REPOSITORY ANALYSIS BEING SENT TO GEMINI ===");
            System.out.println(repoAnalysis);
            System.out.println("=== END REPOSITORY ANALYSIS ===");
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
