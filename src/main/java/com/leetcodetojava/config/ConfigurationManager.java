package com.leetcodetojava.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String CONFIG_FILE = "config.properties";
    
    private Configuration config;
    private String githubToken;
    private String githubRepo;
    private String githubRepoOwner;
    private String githubRepoName;
    
    public ConfigurationManager() {
    }
    
    public boolean loadConfiguration() {
        try {
            Configurations configs = new Configurations();
            File configFile = new File(CONFIG_FILE);
            
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }
            
            this.config = configs.properties(configFile);
            loadValues();
            
            logger.info("Configuration loaded from {}", configFile.getAbsolutePath());
            return true;
            
        } catch (ConfigurationException e) {
            logger.error("Failed to load configuration", e);
            return false;
        }
    }
    
    private void createDefaultConfig(File configFile) {
        try {
            System.out.println("Creating default configuration file...");
            System.out.println("Please edit config.properties with your GitHub settings");
            
            String defaultConfig = "# GitHub Configuration\n" +
                    "github.token=your_github_personal_access_token_here\n" +
                    "github.repo=https://github.com/yourusername/yourrepo\n" +
                    "\n" +
                    "# LeetCode Configuration (optional)\n" +
                    "leetcode.username=your_leetcode_username\n" +
                    "\n" +
                    "# Application Settings\n" +
                    "monitor.interval.seconds=3\n" +
                    "browser.headless=true\n";
            
            Files.write(configFile.toPath(), defaultConfig.getBytes());
            logger.info("Default configuration file created");
            
        } catch (Exception e) {
            logger.error("Failed to create default configuration file", e);
        }
    }
    
    private void loadValues() {
        githubToken = config.getString("github.token", "");
        githubRepo = config.getString("github.repo", "");
        
        if (!githubRepo.isEmpty()) {
            parseGitHubRepo();
        }
    }
    
    private void parseGitHubRepo() {
        try {
            URL url = new URL(githubRepo);
            String path = url.getPath();
            String[] parts = path.split("/");
            
            if (parts.length >= 3) {
                githubRepoOwner = parts[1];
                githubRepoName = parts[2];
            } else {
                logger.error("Invalid GitHub repository URL format: {}", githubRepo);
            }
        } catch (Exception e) {
            logger.error("Failed to parse GitHub repository URL: {}", githubRepo, e);
        }
    }
    
    public boolean validateConfiguration() {
        if (githubToken == null || githubToken.trim().isEmpty()) {
            logger.error("GitHub token is not configured");
            return false;
        }
        
        if (githubRepo == null || githubRepo.trim().isEmpty()) {
            logger.error("GitHub repository is not configured");
            return false;
        }
        
        if (githubRepoOwner == null || githubRepoName == null) {
            logger.error("Invalid GitHub repository URL: {}", githubRepo);
            return false;
        }
        
        // Validate GitHub token format (optional warning)
        if (!java.util.regex.Pattern.matches("^ghp_[a-zA-Z0-9]{36}$|^github_pat_[a-zA-Z0-9]{82}$", githubToken)) {
            logger.warn("GitHub token format doesn't match expected pattern");
        }
        
        logger.info("Configuration validation passed");
        return true;
    }
    
    public String getGitHubToken() {
        return githubToken;
    }
    
    public String getGitHubRepo() {
        return githubRepo;
    }
    
    public String getGitHubRepoOwner() {
        return githubRepoOwner;
    }
    
    public String getGitHubRepoName() {
        return githubRepoName;
    }
    
    public int getMonitorIntervalSeconds() {
        return config.getInt("monitor.interval.seconds", 3);
    }
    
    public boolean isHeadlessMode() {
        return config.getBoolean("browser.headless", true);
    }
    
    public String getLeetCodeUsername() {
        return config.getString("leetcode.username", "");
    }
} 