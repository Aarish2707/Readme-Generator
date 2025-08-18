package com.project.Readme.repository;

import com.project.Readme.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByGithubId(String githubId);
}
