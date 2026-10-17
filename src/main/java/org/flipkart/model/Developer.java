package org.flipkart.model;

import org.flipkart.enums.DeveloperStatus;
import org.flipkart.enums.ProjectCategory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Developer {
    private final String id;
    private final String name;
    private final Set<ProjectCategory> skills;

    private volatile DeveloperStatus status = DeveloperStatus.AVAILABLE;
    private volatile String activeProjectId;
    private volatile double totalRating;
    private volatile int ratingCount;
    private volatile int completedProjects;

    public Developer(String id, String name, Set<ProjectCategory> skills) {
        this.id = id;
        this.name = name;
        this.skills = Collections.unmodifiableSet(new HashSet<>(skills));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<ProjectCategory> getSkills() {
        return skills;
    }

    public DeveloperStatus getStatus() {
        return status;
    }

    public String getActiveProjectId() {
        return activeProjectId;
    }

    public double getAverageRating() {
        return ratingCount == 0 ? 0.0 : totalRating / ratingCount;
    }

    public int getCompletedProjects() {
        return completedProjects;
    }

    public boolean isAvailable() {
        return status == DeveloperStatus.AVAILABLE;
    }

    public void markRequested() {
        status = DeveloperStatus.REQUESTING;
    }

    public void assignProject(String projectId) {
        activeProjectId = projectId;
        status = DeveloperStatus.ASSIGNED;
    }

    public void unAssignProject(String projectId) {
        activeProjectId = null;
        status = DeveloperStatus.AVAILABLE;
    }

    public void startProject(String projectId) {
        activeProjectId = projectId;
        status = DeveloperStatus.IN_PROGRESS;
    }

    public void completeProject() {
        activeProjectId = null;
        status = DeveloperStatus.AVAILABLE;
        completedProjects++;
    }

    public void clearRequestState() {
        if (status == DeveloperStatus.REQUESTING) {
            status = DeveloperStatus.AVAILABLE;
        }
    }

    public void addRating(double rating) {
        totalRating += rating;
        ratingCount++;
    }
}
