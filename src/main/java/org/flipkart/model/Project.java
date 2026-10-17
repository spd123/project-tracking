package org.flipkart.model;

import org.flipkart.enums.ProjectCategory;
import org.flipkart.enums.ProjectStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Project {
    private final String id;
    private final String name;
    private final Lead owner;
    private final Set<ProjectCategory> categories;
    private final Instant createdAt = Instant.now();

    private volatile ProjectStatus status = ProjectStatus.OPEN;
    private volatile String assignedDeveloperId;
    private volatile String acceptedRequestId;

    public Project(String id, String name, Lead owner, Set<ProjectCategory> categories) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.categories = Collections.unmodifiableSet(new HashSet<>(categories));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Lead getOwner() {
        return owner;
    }

    public Set<ProjectCategory> getCategories() {
        return categories;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public String getAssignedDeveloperId() {
        return assignedDeveloperId;
    }

    public String getAcceptedRequestId() {
        return acceptedRequestId;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public void acceptRequest(String requestId, String developerId) {
        acceptedRequestId = requestId;
        assignedDeveloperId = developerId;
        status = ProjectStatus.ASSIGNED;
    }

    public void rejectRequest(String requestId, String developerId) {
        acceptedRequestId = null;
        assignedDeveloperId = null;
        status = ProjectStatus.REQUESTED;
    }

    public void start() {
        status = ProjectStatus.IN_PROGRESS;
    }

    public void complete() {
        status = ProjectStatus.COMPLETED;
    }

    public void cancel() {
        status = ProjectStatus.CANCELED;
    }
}
