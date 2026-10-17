package org.flipkart;

import org.flipkart.enums.ProjectCategory;
import org.flipkart.model.Developer;
import org.flipkart.model.Lead;
import org.flipkart.model.Project;
import org.flipkart.model.ProjectRequest;
import org.flipkart.notification.LocalNotificationListener;
import org.flipkart.service.ProjectTrackerService;
import org.flipkart.strategy.ProjectsBasedRatingStrategy;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        LocalNotificationListener notificationCenter = new LocalNotificationListener();
        try (ProjectTrackerService service = new ProjectTrackerService(new ProjectsBasedRatingStrategy(), notificationCenter)) {
            Scenario scenario = bootstrap(service);
            runAssignmentFlow(service, scenario);
            runCancellationFlow(service, scenario);
            printSummary(service, notificationCenter, scenario);
        }
    }

    private static Scenario bootstrap(ProjectTrackerService service) {
        Lead leadOne = service.registerLead("Lead1");
        Lead leadTwo = service.registerLead("Lead2");
        Developer developerOne = service.registerDeveloper("Dev1", Set.of(ProjectCategory.FRONTEND, ProjectCategory.BACKEND));
        Developer developerTwo = service.registerDeveloper("Dev2", Set.of(ProjectCategory.DEVOPS));
        Project infinityProject = service.registerProject(leadTwo.id(), "Infinity", Set.of(ProjectCategory.FRONTEND, ProjectCategory.BACKEND), Duration.ofSeconds(10));
        Project projectEuler = service.registerProject(leadOne.id(), "Project Euler", Set.of(ProjectCategory.DEVOPS), Duration.ofSeconds(10));
        return new Scenario(leadOne, leadTwo, developerOne, developerTwo, infinityProject, projectEuler);
    }

    private static void runAssignmentFlow(ProjectTrackerService service, Scenario scenario) {
        printProjects("Available projects", service.getAvailableProjects());
        ProjectRequest request = service.requestProject(scenario.developerOne().getId(), scenario.infinityProject().getId());
        System.out.println("Request created: " + request.id());
        service.acceptRequest(scenario.leadTwo().id(), scenario.infinityProject().getId(), request.id());
        service.startProject(scenario.developerOne().getId(), scenario.infinityProject().getId());
        service.completeProject(scenario.developerOne().getId(), scenario.infinityProject().getId());
        service.rateDeveloper(scenario.leadTwo().id(), scenario.infinityProject().getId(), 8.1);
    }

    private static void runCancellationFlow(ProjectTrackerService service, Scenario scenario) {
        service.cancelProject(scenario.leadOne().id(), scenario.projectEuler().getId());
    }

    private static void printSummary(ProjectTrackerService service, LocalNotificationListener notificationCenter, Scenario scenario) {
        printMap("Developer 1", service.getDeveloperDetails(scenario.developerOne().getId()));
        printMap("Developer 2", service.getDeveloperDetails(scenario.developerTwo().getId()));
        printMap("Project 1", service.getProjectDetails(scenario.infinityProject().getId()));
        printMap("Project 2", service.getProjectDetails(scenario.projectEuler().getId()));
        printTopDevelopers(service);
        printNotifications(notificationCenter);
    }

    private static void printTopDevelopers(ProjectTrackerService service) {
        System.out.println("Top developers by completed projects:");
        service.getTopDevelopers().forEach(developer -> System.out.println(
                developer.getName() + " | completed=" + developer.getCompletedProjects() + " | rating=" + String.format("%.2f", developer.getAverageRating())
        ));
    }

    private static void printNotifications(LocalNotificationListener notificationCenter) {
        System.out.println("Notifications:");
        notificationCenter.getNotifications().forEach(System.out::println);
    }

    private static void printProjects(String title, List<Project> projects) {
        System.out.println(title + ":");
        for (Project project : projects) {
            System.out.println(project.getId() + " | " + project.getName() + " | " + project.getCategories() + " | " + project.getOwner().name() + " | " + project.getStatus());
        }
    }

    private static void printMap(String title, java.util.Map<String, String> values) {
        System.out.println(title + ": " + values);
    }

    private record Scenario(Lead leadOne, Lead leadTwo, Developer developerOne, Developer developerTwo,
                            Project infinityProject, Project projectEuler) {
    }
}
