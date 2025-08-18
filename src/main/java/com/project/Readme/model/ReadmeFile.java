package com.project.Readme.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "readmes")
public class ReadmeFile {
    @Id
    private String id;
    private String githubId;
    private String repoName;
    private String ownerName;
    private String content;
    private LocalDateTime createdAt;

    //constructor

    public ReadmeFile(String id, String githubId, String repoName, String ownerName, String content, LocalDateTime createdAt) {
        this.id = id;
        this.githubId = githubId;
        this.repoName = repoName;
        this.ownerName = ownerName;
        this.content = content;
        this.createdAt = createdAt;
    }


    //getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

