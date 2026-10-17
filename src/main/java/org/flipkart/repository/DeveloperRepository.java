package org.flipkart.repository;

import org.flipkart.model.Developer;

import java.util.List;

public interface DeveloperRepository {
    void save(Developer developer);

    Developer findById(String developerId);

    List<Developer> findAll();
}
