package org.flipkart.repository;

import org.flipkart.model.Project;

import java.util.List;

public interface ProjectRepository {
    void save(Project project);

    Project findById(String projectId);

    List<Project> findAll();
}
