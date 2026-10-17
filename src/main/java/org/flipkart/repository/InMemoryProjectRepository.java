package org.flipkart.repository;

import org.flipkart.model.Project;

public final class InMemoryProjectRepository extends AbstractInMemoryRepository<Project> implements ProjectRepository {
    @Override
    public void save(Project project) {
        save(project.getId(), project);
    }

    @Override
    public Project findById(String projectId) {
        return lookup(projectId);
    }

    @Override
    public java.util.List<Project> findAll() {
        return snapshot();
    }
}
