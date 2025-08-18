package com.project.Readme.repository;

import com.project.Readme.model.ReadmeFile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReadmeRepository extends MongoRepository<ReadmeFile, String> {
    List<ReadmeFile> findByGithubId(String githubId);
}
