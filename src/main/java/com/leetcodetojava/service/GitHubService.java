package com.leetcodetojava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcodetojava.config.ConfigurationManager;
import com.leetcodetojava.model.Submission;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    
    private final ConfigurationManager configManager;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    
    public GitHubService(ConfigurationManager configManager) {
        this.configManager = configManager;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public boolean uploadSubmission(Submission submission) {
        try {
            String filePath = buildFilePath(submission);
            String content = Base64.getEncoder().encodeToString(
                    submission.getCode().getBytes(StandardCharsets.UTF_8)
            );
            
            String existingSha = getFileSha(filePath);
            String requestBody;
            String commitMessage;
            
            if (existingSha != null) {
                // Update existing file
                UpdateFileRequest updateRequest = new UpdateFileRequest(
                        "feat: update " + submission.getLanguage() + " solution for " + submission.getProblemName(),
                        content,
                        existingSha
                );
                requestBody = objectMapper.writeValueAsString(updateRequest);
                commitMessage = "Updated " + submission.getLanguage() + " solution for " + submission.getProblemName();
            } else {
                // Create new file
                CreateFileRequest createRequest = new CreateFileRequest(
                        "feat: add " + submission.getLanguage() + " solution for " + submission.getProblemName(),
                        content
                );
                requestBody = objectMapper.writeValueAsString(createRequest);
                commitMessage = "Added " + submission.getLanguage() + " solution for " + submission.getProblemName();
            }
            
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s",
                    configManager.getGitHubRepoOwner(),
                    configManager.getGitHubRepoName(),
                    filePath);
            
            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .addHeader("Authorization", "Bearer " + configManager.getGitHubToken())
                    .build();
            
            Response response = null;
            try {
                response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    JsonNode responseBody = objectMapper.readTree(response.body().string());
                    String commitUrl = responseBody.get("content").get("html_url").asText();
                    
                    logger.info("Successfully uploaded: {}", commitMessage);
                    logger.info("File URL: {}", commitUrl);
                    System.out.println("✅ Uploaded: " + commitMessage);
                    
                    return true;
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("Failed to upload to GitHub. Status: {}, Body: {}", response.code(), errorBody);
                    System.out.println("❌ Failed to upload: " + submission.getProblemName());
                    return false;
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error uploading submission to GitHub", e);
            System.out.println("❌ Error uploading: " + e.getMessage());
            return false;
        }
    }
    
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                    .url("https://api.github.com/user")
                    .get()
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .addHeader("Authorization", "Bearer " + configManager.getGitHubToken())
                    .build();
            
            Response response = null;
            try {
                response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    JsonNode user = objectMapper.readTree(response.body().string());
                    logger.info("GitHub connection successful. User: {}", user.get("login").asText());
                    return true;
                } else {
                    logger.error("GitHub connection failed. Status: {}", response.code());
                    return false;
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error testing GitHub connection", e);
            return false;
        }
    }
    
    private String getFileSha(String filePath) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s",
                    configManager.getGitHubRepoOwner(),
                    configManager.getGitHubRepoName(),
                    filePath);
            
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .addHeader("Authorization", "Bearer " + configManager.getGitHubToken())
                    .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful()) {
                JsonNode fileInfo = objectMapper.readTree(response.body().string());
                return fileInfo.get("sha").asText();
            } else {
                logger.debug("File does not exist or error checking: {}", filePath);
                return null;
            }
            
        } catch (Exception e) {
            logger.debug("Error checking file existence: {}", filePath);
            return null;
        }
    }
    
    private String buildFilePath(Submission submission) {
        String extension = getLanguageExtension(submission.getLanguage());
        return String.format("%s/%s.%s", submission.getLanguage().toLowerCase(), submission.getProblemName(), extension);
    }
    
    private String getLanguageExtension(String language) {
        if (language == null) return "txt";
        
        String lang = language.toLowerCase();
        
        switch (lang) {
            case "python":
            case "python3":
            case "py":
                return "py";
            case "java":
                return "java";
            case "javascript":
            case "js":
                return "js";
            case "typescript":
            case "ts":
                return "ts";
            case "c++":
            case "cpp":
                return "cpp";
            case "c":
                return "c";
            case "c#":
            case "csharp":
            case "cs":
                return "cs";
            case "go":
            case "golang":
                return "go";
            case "rust":
            case "rs":
                return "rs";
            case "ruby":
            case "rb":
                return "rb";
            case "php":
                return "php";
            case "swift":
                return "swift";
            case "kotlin":
            case "kt":
                return "kt";
            case "scala":
                return "scala";
            case "dart":
                return "dart";
            case "elixir":
            case "ex":
                return "ex";
            case "erlang":
            case "erl":
                return "erl";
            case "racket":
            case "rkt":
                return "rkt";
            default:
                return "txt";
        }
    }
    
    public ConfigurationManager getConfigManager() {
        return configManager;
    }
    
    public static class CreateFileRequest {
        public String message;
        public String content;
        
        public CreateFileRequest(String message, String content) {
            this.message = message;
            this.content = content;
        }
    }
    
    public static class UpdateFileRequest {
        public String message;
        public String content;
        public String sha;
        
        public UpdateFileRequest(String message, String content, String sha) {
            this.message = message;
            this.content = content;
            this.sha = sha;
        }
    }
} 