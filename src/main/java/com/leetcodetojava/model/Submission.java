package com.leetcodetojava.model;

public class Submission {
    private String problemName;
    private String language;
    private String code;
    private String submissionId;
    private String status;
    private long timestamp;
    
    public Submission() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Submission(String problemName, String language, String code) {
        this();
        this.problemName = problemName;
        this.language = language;
        this.code = code;
    }
    
    public String getProblemName() {
        return problemName;
    }
    
    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getSubmissionId() {
        return submissionId;
    }
    
    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Submission{" +
                "problemName='" + problemName + '\'' +
                ", language='" + language + '\'' +
                ", submissionId='" + submissionId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 