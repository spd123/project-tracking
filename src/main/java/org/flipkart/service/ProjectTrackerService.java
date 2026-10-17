package org.flipkart.service;

import org.flipkart.enums.ProjectCategory;
import org.flipkart.enums.ProjectStatus;
import org.flipkart.model.Developer;
import org.flipkart.model.Lead;
import org.flipkart.model.Project;
import org.flipkart.model.ProjectRequest;
import org.flipkart.notification.LocalNotificationListener;
import org.flipkart.notification.NotificationListener;
import org.flipkart.repository.*;
import org.flipkart.strategy.DeveloperRatingStrategy;
import org.flipkart.strategy.ProjectsBasedRatingStrategy;
import org.flipkart.util.IdGenerationService;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ProjectTrackerService implements AutoCloseable {
    private final LeadRepository leadRepository;
    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRequestRepository projectRequestRepository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DeveloperRatingStrategy developerRatingStrategy;
    private final NotificationListener notificationListener;

    public ProjectTrackerService() {
        this(new ProjectsBasedRatingStrategy(), new LocalNotificationListener());
    }

    public ProjectTrackerService(DeveloperRatingStrategy developerRatingStrategy) {
        this(developerRatingStrategy, new LocalNotificationListener());
    }

    public ProjectTrackerService(DeveloperRatingStrategy developerRatingStrategy, NotificationListener notificationListener) {
        this(new InMemoryLeadRepository(),
                new InMemoryDeveloperRepository(),
                new InMemoryProjectRepository(),
                new InMemoryProjectRequestRepository(),
                developerRatingStrategy,
                notificationListener);
    }

    public ProjectTrackerService(LeadRepository leadRepository,
                                 DeveloperRepository developerRepository,
                                 ProjectRepository projectRepository,
                                 ProjectRequestRepository projectRequestRepository,
                                 DeveloperRatingStrategy developerRatingStrategy,
                                 NotificationListener notificationListener) {
        this.leadRepository = leadRepository;
        this.developerRepository = developerRepository;
        this.projectRepository = projectRepository;
        this.projectRequestRepository = projectRequestRepository;
        this.developerRatingStrategy = developerRatingStrategy;
        this.notificationListener = notificationListener;
    }

    public Lead registerLead(String name) {
        Lead lead = new Lead(IdGenerationService.nextLeadId(), name);
        leadRepository.save(lead);
        return lead;
    }

    public Developer registerDeveloper(String name, Set<ProjectCategory> skills) {
        Developer developer = new Developer(IdGenerationService.nextDeveloperId(), name, skills);
        developerRepository.save(developer);
        return developer;
    }

    public Project registerProject(String leadId, String projectName, Set<ProjectCategory> categories, Duration autoCancelAfter) {
        Lead lead = requireLead(leadId);
        String projectId = IdGenerationService.nextProjectId();
        Project project = new Project(projectId, projectName, lead, categories);
        projectRepository.save(project);
        if (autoCancelAfter != null && !autoCancelAfter.isZero() && !autoCancelAfter.isNegative()) {
            scheduler.schedule(() -> autoCancelIfStillOpen(projectId), autoCancelAfter.toMillis(), TimeUnit.MILLISECONDS);
        }
        return project;
    }

    public List<Project> getAvailableProjects() {
        return projectRepository.findAll().stream()
                .filter(project -> project.getStatus() == ProjectStatus.OPEN || project.getStatus() == ProjectStatus.REQUESTED)
                .sorted(Comparator.comparing(Project::getCreatedAt))
                .toList();
    }

    public ProjectRequest requestProject(String developerId, String projectId) {
        Developer developer = requireDeveloper(developerId);
        Project project = requireProject(projectId);
        return withProjectLock(project, () -> {
            ensureProjectIsRequestable(project, projectId);
            ensureDeveloperIsFree(developer, developerId);
            developer.markRequested();
            project.setStatus(ProjectStatus.REQUESTED);
            ProjectRequest request = new ProjectRequest(IdGenerationService.nextRequestId(), projectId, developerId, java.time.Instant.now());
            projectRequestRepository.save(request);
            notifyProjectChange(project, "requested by " + developer.getName());
            return request;
        });
    }

    public void acceptRequest(String leadId, String projectId, String requestId) {
        Lead lead = requireLead(leadId);
        Project project = requireProject(projectId);
        ProjectRequest request = requireProjectRequest(requestId);
        Developer developer = requireDeveloper(request.developerId());
        try {

            ensureProjectOwnership(project, lead);
            ensureRequestMatchesProject(request, projectId);
            withProjectLock(project, () -> {
                ensureProjectAssignable(project);
                project.acceptRequest(request.id(), developer.getId());
                developer.assignProject(project.getId());
                removeCompetingRequests(projectId, requestId);
                notifyProjectChange(project, "assigned to " + developer.getName());
                return null;
            });
        } catch (Exception ex) {
            project.rejectRequest(request.id(), developer.getId());
            developer.unAssignProject(project.getId());

        }
    }

    public void startProject(String developerId, String projectId) {
        Developer developer = requireDeveloper(developerId);
        Project project = requireProject(projectId);
        withProjectLock(project, () -> {
            ensureDeveloperAssignment(project, developer);
            ensureProjectStatus(project, ProjectStatus.ASSIGNED, "Project must be assigned before it can be started");
            project.start();
            developer.startProject(projectId);
            notifyProjectChange(project, "started by " + developer.getName());
            return null;
        });
    }

    public void completeProject(String developerId, String projectId) {
        Developer developer = requireDeveloper(developerId);
        Project project = requireProject(projectId);
        withProjectLock(project, () -> {
            ensureDeveloperAssignment(project, developer);
            ensureProjectStatus(project, ProjectStatus.IN_PROGRESS, "Project must be in progress before it can be completed");
            project.complete();
            developer.completeProject();
            notifyProjectChange(project, "completed by " + developer.getName());
            return null;
        });
    }

    public void cancelProject(String leadId, String projectId) {
        Lead lead = requireLead(leadId);
        Project project = requireProject(projectId);
        ensureProjectOwnership(project, lead);
        withProjectLock(project, () -> {
            if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                throw new IllegalStateException("Cannot cancel an in-progress project");
            }
            if (project.getStatus() == ProjectStatus.COMPLETED) {
                throw new IllegalStateException("Cannot cancel a completed project");
            }
            project.cancel();
            clearProjectRequests(projectId);
            notifyProjectChange(project, "canceled by " + lead.name());
            return null;
        });
    }

    public void rateDeveloper(String leadId, String projectId, double rating) {
        Lead lead = requireLead(leadId);
        Project project = requireProject(projectId);
        ensureProjectOwnership(project, lead);
        ensureProjectStatus(project, ProjectStatus.COMPLETED, "Ratings are allowed only after completion");
        Developer developer = requireDeveloper(project.getAssignedDeveloperId());
        developer.addRating(rating);
    }

    public Map<String, String> getDeveloperDetails(String developerId) {
        Developer developer = requireDeveloper(developerId);
        Map<String, String> details = new LinkedHashMap<>();
        details.put("developer", developer.getName());
        details.put("status", developer.getStatus().name());
        details.put("activeProjectId", String.valueOf(developer.getActiveProjectId()));
        details.put("completedProjects", String.valueOf(developer.getCompletedProjects()));
        details.put("averageRating", String.format("%.2f", developer.getAverageRating()));
        return details;
    }

    public Map<String, String> getProjectDetails(String projectId) {
        Project project = requireProject(projectId);
        Map<String, String> details = new LinkedHashMap<>();
        details.put("project", project.getName());
        details.put("status", project.getStatus().name());
        details.put("owner", project.getOwner().name());
        details.put("assignedDeveloperId", String.valueOf(project.getAssignedDeveloperId()));
        return details;
    }

    public List<Developer> getTopDevelopers() {
        return getTopDevelopers(developerRatingStrategy);
    }

    public List<Developer> getTopDevelopers(DeveloperRatingStrategy rankingStrategy) {
        return developerRepository.findAll().stream()
                .sorted(rankingStrategy.comparator())
                .toList();
    }

    private void autoCancelIfStillOpen(String projectId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            return;
        }
        withProjectLock(project, () -> {
            if (project.getStatus() == ProjectStatus.OPEN || project.getStatus() == ProjectStatus.REQUESTED) {
                project.cancel();
                clearProjectRequests(projectId);
                notifyProjectChange(project, "auto-canceled after inactivity");
            }
            return null;
        });
    }

    private void clearProjectRequests(String projectId) {
        purgeRequests(projectId, null);
    }

    private void removeCompetingRequests(String projectId, String acceptedRequestId) {
        purgeRequests(projectId, acceptedRequestId);
    }



    private void purgeRequests(String projectId, String requestToKeep) {
        for (ProjectRequest request : projectRequestRepository.findByProjectId(projectId)) {
            if (requestToKeep != null && request.id().equals(requestToKeep)) {
                continue;
            }
            projectRequestRepository.remove(request.id());
            Developer developer = developerRepository.findById(request.developerId());
            if (developer != null) {
                developer.clearRequestState();
            }
        }
    }



    private Lead requireLead(String leadId) {
        return require("lead", leadId, () -> leadRepository.findById(leadId));
    }

    private Developer requireDeveloper(String developerId) {
        return require("developer", developerId, () -> developerRepository.findById(developerId));
    }

    private Project requireProject(String projectId) {
        return require("project", projectId, () -> projectRepository.findById(projectId));
    }

    private ProjectRequest requireProjectRequest(String requestId) {
        return require("request", requestId, () -> projectRequestRepository.findById(requestId));
    }

    private <T> T require(String label, String id, Supplier<T> lookup) {
        T value = lookup.get();
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + label + ": " + id);
        }
        return value;
    }

    private void notifyProjectChange(Project project, String message) {
        if (notificationListener != null) {
            notificationListener.onProjectStatusChanged(project, message);
        }
    }

    private void ensureProjectOwnership(Project project, Lead lead) {
        if (!Objects.equals(project.getOwner().id(), lead.id())) {
            throw new IllegalArgumentException("Lead does not own this project");
        }
    }

    private void ensureRequestMatchesProject(ProjectRequest request, String projectId) {
        if (!Objects.equals(request.projectId(), projectId)) {
            throw new IllegalArgumentException("Request does not belong to this project");
        }
    }

    private void ensureProjectAssignable(Project project) {
        if (project.getStatus() == ProjectStatus.CANCELED) {
            throw new IllegalStateException("Cannot accept a canceled project");
        }
        if (project.getAssignedDeveloperId() != null) {
            throw new IllegalStateException("Project already has an accepted request");
        }
    }

    private void ensureProjectIsRequestable(Project project, String projectId) {
        if (project.getStatus() != ProjectStatus.OPEN && project.getStatus() != ProjectStatus.REQUESTED) {
            throw new IllegalStateException("Project is no longer available: " + projectId);
        }
    }

    private void ensureDeveloperIsFree(Developer developer, String developerId) {
        if (!developer.isAvailable()) {
            throw new IllegalStateException("Developer is already occupied: " + developerId);
        }
    }

    private void ensureDeveloperAssignment(Project project, Developer developer) {
        if (!Objects.equals(project.getAssignedDeveloperId(), developer.getId())) {
            throw new IllegalStateException("Project is not assigned to this developer");
        }
    }

    private void ensureProjectStatus(Project project, ProjectStatus expected, String message) {
        if (project.getStatus() != expected) {
            throw new IllegalStateException(message);
        }
    }

    private <T> T withProjectLock(Project project, Supplier<T> operation) {
        synchronized (project) {
            return operation.get();
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
