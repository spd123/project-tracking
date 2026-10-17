package org.flipkart.repository;

import org.flipkart.model.ProjectRequest;

import java.util.List;

public interface ProjectRequestRepository {
    void save(ProjectRequest request);

    ProjectRequest findById(String requestId);

    List<ProjectRequest> findByProjectId(String projectId);

    void remove(String requestId);
}
