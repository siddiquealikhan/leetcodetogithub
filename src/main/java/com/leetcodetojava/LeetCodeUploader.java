package com.leetcodetojava;

import com.leetcodetojava.config.ConfigurationManager;
import com.leetcodetojava.service.GitHubService;
import com.leetcodetojava.service.LeetCodeMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LeetCodeUploader {
    private static final Logger logger = LoggerFactory.getLogger(LeetCodeUploader.class);
    
    private final ConfigurationManager configManager;
    private final LeetCodeMonitorService monitorService;
    private final GitHubService githubService;
    private final ScheduledExecutorService executor;
    
    public LeetCodeUploader() {
        this.configManager = new ConfigurationManager();
        this.githubService = new GitHubService(configManager);
        this.monitorService = new LeetCodeMonitorService(githubService);
        this.executor = Executors.newScheduledThreadPool(1);
    }
    
    public static void main(String[] args) {
        System.out.println("=== LeetCode Uploader ===");
        System.out.println("Starting LeetCode submission monitor...");
        System.out.println("Keep this program running while solving LeetCode problems");
        System.out.println("Press Ctrl+C to stop the program");
        System.out.println();
        
        LeetCodeUploader uploader = new LeetCodeUploader();
        uploader.start();
    }
    
    public void start() {
        try {
            if (!configManager.loadConfiguration()) {
                logger.error("Failed to load configuration. Please check config.properties file.");
                System.exit(1);
            }
            
            if (!configManager.validateConfiguration()) {
                logger.error("Invalid configuration. Please check your GitHub token and repository settings.");
                System.exit(1);
            }
            
            logger.info("Configuration loaded successfully");
            logger.info("GitHub Repository: {}", configManager.getGitHubRepo());
            logger.info("Monitoring LeetCode submissions...");
            
            startMonitoring();
            
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
        } catch (Exception e) {
            logger.error("Failed to start LeetCode Uploader", e);
            System.exit(1);
        }
    }
    
    private void startMonitoring() {
        executor.scheduleAtFixedRate(() -> {
            try {
                monitorService.checkForSubmissions();
            } catch (Exception e) {
                logger.error("Error during submission monitoring", e);
            }
        }, 0, 3, TimeUnit.SECONDS);
        
        logger.info("Monitoring started. Checking for submissions every 3 seconds...");
    }
    
    public void shutdown() {
        logger.info("Shutting down LeetCode Uploader...");
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("LeetCode Uploader stopped.");
    }
} 