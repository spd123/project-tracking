package org.flipkart.repository;

import org.flipkart.model.ProjectRequest;

import java.util.List;

public final class InMemoryProjectRequestRepository extends AbstractInMemoryRepository<ProjectRequest> implements ProjectRequestRepository {
    @Override
    public void save(ProjectRequest request) {
        save(request.id(), request);
    }

    @Override
    public ProjectRequest findById(String requestId) {
        return lookup(requestId);
    }

    @Override
    public List<ProjectRequest> findByProjectId(String projectId) {
        return snapshot().stream()
                .filter(request -> request.projectId().equals(projectId))
                .toList();
    }

    @Override
    public void remove(String requestId) {
        delete(requestId);
    }
}
