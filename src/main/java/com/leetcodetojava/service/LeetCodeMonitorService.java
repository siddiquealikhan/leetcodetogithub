package com.leetcodetojava.service;

import com.leetcodetojava.config.ConfigurationManager;
import com.leetcodetojava.model.Submission;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeetCodeMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(LeetCodeMonitorService.class);
    private static final String LEETCODE_BASE_URL = "https://leetcode.com";
    
    private final GitHubService githubService;
    private final ConfigurationManager configManager;
    private WebDriver driver;
    private WebDriverWait wait;
    private final Map<String, Submission> pendingSubmissions;
    private final AtomicBoolean isMonitoring;
    private String currentProblemName;
    private final Set<String> processedSubmissions; // Track processed submissions to avoid reprocessing
    
    public LeetCodeMonitorService(GitHubService githubService) {
        this.githubService = githubService;
        this.configManager = githubService.getConfigManager();
        this.pendingSubmissions = new HashMap<>();
        this.processedSubmissions = new HashSet<>();
        this.isMonitoring = new AtomicBoolean(false);
    }
    
    private void initializeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");
        
        if (configManager.isHeadlessMode()) {
            options.addArguments("--headless");
        } else {
            logger.info("Running in visible mode - Chrome window should be visible");
        }
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        logger.info("WebDriver initialized successfully");
    }
    
    public void checkForSubmissions() {
        try {
            if (driver == null) {
                initializeDriver();
                startMonitoring();
            }
            
            // Check if session is still valid
            try {
                driver.getCurrentUrl();
            } catch (Exception e) {
                logger.warn("Browser session invalid, reinitializing driver");
                driver = null;
                initializeDriver();
                startMonitoring();
                return;
            }
            
            String currentUrl = driver.getCurrentUrl();
            
            if (isOnLeetCodeProblemPage()) {
                // First check for new submissions (code in editor), then check results
                checkForNewSubmissions();
                checkForSubmissionResults();
            } else if (currentUrl.contains("/submissions/")) {
                checkForSubmissionResultsOnSubmissionsPage();
            } else {
                // Navigate to LeetCode if not already there
                if (!currentUrl.contains("leetcode.com")) {
                    driver.get(LEETCODE_BASE_URL);
                    logger.info("Navigated to LeetCode");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error checking for submissions", e);
            // If there's a session error, reset the driver
            if (e.getMessage() != null && e.getMessage().contains("invalid session")) {
                logger.info("Detected session error, will reinitialize driver on next check");
                driver = null;
            }
        }
    }
    
    private void startMonitoring() {
        driver.get(LEETCODE_BASE_URL);
        logger.info("Navigated to LeetCode");
        isMonitoring.set(true);
    }
    
    private boolean isOnLeetCodeProblemPage() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("/problems/") && !currentUrl.contains("/submissions/");
    }
    
    private boolean checkForSubmissionResultsOnSubmissionsPage() {
        try {
            String problemName = extractProblemNameFromUrl(driver.getCurrentUrl());
            
            // Check if we've already processed this submission
            if (processedSubmissions.contains(problemName)) {
                logger.debug("Already processed submission for: {}, skipping", problemName);
                // Still redirect to main problem page to avoid staying on submissions page
                String mainProblemUrl = extractMainProblemUrl(driver.getCurrentUrl());
                driver.get(mainProblemUrl);
                return false;
            }
            
            // Look for accepted submissions
            List<WebElement> acceptedSubmissions = driver.findElements(By.xpath("//span[contains(text(), 'Accepted')]"));
            
            if (!acceptedSubmissions.isEmpty()) {
                logger.info("Found accepted submission on submissions page for: {}", problemName);
                
                // Set current problem name for processing
                currentProblemName = problemName;
                
                // Mark this submission as processed to avoid reprocessing
                processedSubmissions.add(problemName);
                
                // Process the accepted submission
                handleAcceptedSubmission();
                
                // Redirect to main problem page
                String mainProblemUrl = extractMainProblemUrl(driver.getCurrentUrl());
                driver.get(mainProblemUrl);
                logger.info("Processed submission result, redirecting to main problem page: {}", mainProblemUrl);
                return true;
            }
            
            // If no accepted submissions found, redirect to main problem page
            String mainProblemUrl = extractMainProblemUrl(driver.getCurrentUrl());
            driver.get(mainProblemUrl);
            logger.info("Redirecting from submissions page to main problem page: {}", mainProblemUrl);
            
        } catch (Exception e) {
            logger.error("Error checking submission results on submissions page", e);
        }
        
        return false;
    }
    
    private String extractMainProblemUrl(String submissionsUrl) {
        // Convert submissions URL to main problem URL
        // Example: https://leetcode.com/submissions/detail/123456/ -> https://leetcode.com/problems/two-sum/
        if (submissionsUrl.contains("/submissions/detail/")) {
            // Extract problem name from URL or page
            String problemName = extractProblemNameFromUrl(submissionsUrl);
            return LEETCODE_BASE_URL + "/problems/" + problemName + "/";
        }
        return submissionsUrl;
    }
    
    private String extractProblemNameFromUrl(String url) {
        // Extract problem name from various URL formats
        if (url.contains("/problems/")) {
            String[] parts = url.split("/problems/");
            if (parts.length > 1) {
                String problemPart = parts[1];
                return problemPart.split("/")[0];
            }
        }
        return "unknown";
    }
    
    private String extractProblemName() {
        try {
            // Try to extract problem name from the page title or URL
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.contains("/problems/")) {
                return extractProblemNameFromUrl(currentUrl);
            }
            
            // Try to get from page title
            String title = driver.getTitle();
            if (title.contains(" - LeetCode")) {
                return title.split(" - LeetCode")[0].trim();
            }
            
        } catch (Exception e) {
            logger.error("Error extracting problem name", e);
        }
        
        return "unknown";
    }
    
    private void checkForSubmissionResults() {
        try {
            // Check if we're on a problem page and look for submission results
            String problemName = extractProblemName();
            
            if (!"unknown".equals(problemName)) {
                currentProblemName = problemName;
                logger.info("Detected problem: {}", problemName);
            }
            
            // Look for submission status indicators
            List<WebElement> statusElements = driver.findElements(By.xpath("//div[contains(@class, 'status')]"));
            
            for (WebElement statusElement : statusElements) {
                String status = statusElement.getText();
                if ("Accepted".equals(status)) {
                    handleAcceptedSubmission();
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error checking submission results", e);
        }
    }
    
    private void checkForNewSubmissions() {
        try {
            // Only proceed if we have a valid problem name
            if (currentProblemName == null || "unknown".equals(currentProblemName)) {
                return;
            }
            
            // Check if we already have a pending submission for this problem
            if (pendingSubmissions.containsKey(currentProblemName)) {
                // Update existing submission with latest code
                Submission existingSubmission = pendingSubmissions.get(currentProblemName);
                String code = extractCodeFromEditor();
                if (code != null && !code.trim().isEmpty()) {
                    existingSubmission.setCode(cleanExtractedCode(code));
                    logger.debug("Updated pending submission code for: {}", currentProblemName);
                }
                return;
            }
            
            // Look for code in the editor
            String code = extractCodeFromEditor();
            
            if (code != null && !code.trim().isEmpty()) {
                // Create a new pending submission
                Submission submission = new Submission();
                submission.setProblemName(currentProblemName);
                submission.setLanguage(detectProgrammingLanguage());
                submission.setCode(cleanExtractedCode(code));
                submission.setStatus("Pending");
                
                pendingSubmissions.put(currentProblemName, submission);
                logger.info("Created pending submission for: {}", currentProblemName);
            }
            
        } catch (Exception e) {
            logger.error("Error checking for new submissions", e);
        }
    }
    
    private void handleAcceptedSubmission() {
        try {
            if (currentProblemName == null || "unknown".equals(currentProblemName)) {
                logger.warn("Cannot handle accepted submission: no valid problem name");
                return;
            }
            
            logger.info("Handling accepted submission for: {}", currentProblemName);
            
            Submission submission;
            
            if (pendingSubmissions.containsKey(currentProblemName)) {
                // Use existing pending submission
                submission = pendingSubmissions.get(currentProblemName);
                submission.setStatus("Accepted");
                logger.info("Processing existing pending submission for: {}", currentProblemName);
            } else {
                // Create a new submission from current editor state
                String code = extractCodeFromEditor();
                if (code == null || code.trim().isEmpty()) {
                    logger.warn("No code found in editor for accepted submission: {}", currentProblemName);
                    return;
                }
                
                submission = new Submission();
                submission.setProblemName(currentProblemName);
                submission.setLanguage(detectProgrammingLanguage());
                submission.setCode(cleanExtractedCode(code));
                submission.setStatus("Accepted");
                logger.info("Created new submission from editor for: {}", currentProblemName);
            }
            
            logger.info("About to upload submission to GitHub: {}", submission.getProblemName());
            
            // Upload to GitHub
            boolean success = githubService.uploadSubmission(submission);
            
            if (success) {
                pendingSubmissions.remove(currentProblemName);
                processedSubmissions.add(currentProblemName);
                logger.info("Successfully processed and uploaded submission for: {}", currentProblemName);
            } else {
                logger.error("Failed to upload submission for: {}", currentProblemName);
            }
            
        } catch (Exception e) {
            logger.error("Error handling accepted submission", e);
        }
    }
    
    private String extractCodeFromEditor() {
        try {
            // Try multiple selectors for LeetCode's code editor
            // Modern LeetCode uses Monaco editor
            List<WebElement> monacoElements = driver.findElements(By.xpath("//div[contains(@class, 'monaco-editor')]//textarea"));
            if (!monacoElements.isEmpty()) {
                String code = monacoElements.get(0).getAttribute("value");
                if (code != null && !code.trim().isEmpty()) {
                    logger.debug("Extracted code from Monaco editor");
                    return code;
                }
            }
            
            // Try Monaco editor with different selector
            List<WebElement> monacoTextareas = driver.findElements(By.xpath("//textarea[@data-cy='code-editor']"));
            if (!monacoTextareas.isEmpty()) {
                String code = monacoTextareas.get(0).getAttribute("value");
                if (code != null && !code.trim().isEmpty()) {
                    logger.debug("Extracted code from Monaco editor (data-cy selector)");
                    return code;
                }
            }
            
            // Try CodeMirror (older LeetCode editor)
            List<WebElement> codeMirrorElements = driver.findElements(By.xpath("//pre[@class='CodeMirror-line']"));
            if (!codeMirrorElements.isEmpty()) {
                StringBuilder code = new StringBuilder();
                for (WebElement element : codeMirrorElements) {
                    code.append(element.getText()).append("\n");
                }
                String result = code.toString().trim();
                if (!result.isEmpty()) {
                    logger.debug("Extracted code from CodeMirror editor");
                    return result;
                }
            }
            
            // Try Ace editor
            List<WebElement> aceElements = driver.findElements(By.xpath("//div[@class='ace_editor']//textarea"));
            if (!aceElements.isEmpty()) {
                String code = aceElements.get(0).getAttribute("value");
                if (code != null && !code.trim().isEmpty()) {
                    logger.debug("Extracted code from Ace editor");
                    return code;
                }
            }
            
            // Try generic code editor selectors
            List<WebElement> genericElements = driver.findElements(By.xpath("//div[contains(@class, 'editor')]//textarea"));
            if (!genericElements.isEmpty()) {
                String code = genericElements.get(0).getAttribute("value");
                if (code != null && !code.trim().isEmpty()) {
                    logger.debug("Extracted code from generic editor");
                    return code;
                }
            }
            
            logger.debug("No code found in editor with any selector");
            
        } catch (Exception e) {
            logger.error("Error extracting code from editor", e);
        }
        
        return null;
    }
    
    private void extractCodeFromSubmissionPage() {
        try {
            // Navigate to submissions page to extract code
            String submissionsUrl = driver.getCurrentUrl().replace("/problems/", "/submissions/");
            driver.get(submissionsUrl);
            
            // Extract code from submission page
            String code = extractCodeFromEditor();
            if (code != null) {
                // Process the extracted code
                logger.info("Extracted code from submission page");
            }
            
        } catch (Exception e) {
            logger.error("Error extracting code from submission page", e);
        }
    }
    
    private String cleanExtractedCode(String code) {
        if (code == null) return "";
        
        // Remove extra whitespace and normalize line endings
        return code.trim().replaceAll("\r\n", "\n");
    }
    
    private String detectProgrammingLanguageFromPage() {
        try {
            // Look for language indicators on the page
            List<WebElement> languageElements = driver.findElements(By.xpath("//select[@data-cy='lang-select']//option[@selected]"));
            
            if (!languageElements.isEmpty()) {
                return languageElements.get(0).getText();
            }
            
            // Alternative selectors
            List<WebElement> langElements = driver.findElements(By.xpath("//div[contains(@class, 'language')]"));
            if (!langElements.isEmpty()) {
                return langElements.get(0).getText();
            }
            
        } catch (Exception e) {
            logger.error("Error detecting programming language from page", e);
        }
        
        return "java"; // Default fallback
    }
    
    private String detectProgrammingLanguage() {
        String detectedLang = detectProgrammingLanguageFromPage();
        return mapLanguageName(detectedLang);
    }
    
    private String mapLanguageName(String language) {
        if (language == null) return "java";
        
        String lang = language.toLowerCase();
        switch (lang) {
            case "python":
            case "python3":
                return "python";
            case "java":
                return "java";
            case "javascript":
            case "js":
                return "javascript";
            case "typescript":
            case "ts":
                return "typescript";
            case "c++":
            case "cpp":
                return "cpp";
            case "c":
                return "c";
            case "c#":
            case "csharp":
                return "csharp";
            case "go":
            case "golang":
                return "golang";
            case "rust":
                return "rust";
            case "ruby":
                return "ruby";
            case "php":
                return "php";
            case "swift":
                return "swift";
            case "kotlin":
                return "kotlin";
            case "scala":
                return "scala";
            case "dart":
                return "dart";
            default:
                return "java";
        }
    }
    
    public void shutdown() {
        try {
            isMonitoring.set(false);
            if (driver != null) {
                driver.quit();
                driver = null;
            }
            logger.info("LeetCodeMonitorService shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
} 