package org.flipkart;

import org.flipkart.enums.ProjectCategory;
import org.flipkart.model.Developer;
import org.flipkart.model.Lead;
import org.flipkart.model.Project;
import org.flipkart.model.ProjectRequest;
import org.flipkart.service.ProjectTrackerService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectTrackerServiceTest {

    @Test
    void availabilityReflectsFreshlyRegisteredProject() {
        withService(service -> {
            Fixture fixture = Fixture.bootstrap(service);

            List<Project> available = service.getAvailableProjects();

            assertEquals(3, available.size());
            assertEquals(fixture.infinityProject().getId(), available.get(0).getId());
            assertEquals(fixture.projectEuler().getId(), available.get(1).getId());
            assertEquals(fixture.infrastructureTwo().getId(), available.get(2).getId());
        });
    }

    @Test
    void assignmentFlowEndsWithClosedProjectAndFreedDeveloper() {
        withService(service -> {
            Fixture fixture = Fixture.bootstrap(service);

            ProjectRequest request = service.requestProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId());
            service.acceptRequest(fixture.appLead().id(), fixture.infinityProject().getId(), request.id());
            service.startProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId());
            service.completeProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId());
            service.rateDeveloper(fixture.appLead().id(), fixture.infinityProject().getId(), 4.5);

            assertEquals("COMPLETED", service.getProjectDetails(fixture.infinityProject().getId()).get("status"));
            assertEquals("AVAILABLE", service.getDeveloperDetails(fixture.backendDeveloper().getId()).get("status"));
            assertEquals("1", service.getDeveloperDetails(fixture.backendDeveloper().getId()).get("completedProjects"));
            assertEquals("4.50", service.getDeveloperDetails(fixture.backendDeveloper().getId()).get("averageRating"));
        });
    }

    @Test
    void aDeveloperCannotCollectTwoOpenRequestsAtOnce() {
        withService(service -> {
            Fixture fixture = Fixture.bootstrap(service);

            service.requestProject(fixture.devOpsDeveloper().getId(), fixture.projectEuler().getId());

            assertThrows(IllegalStateException.class, () ->
                    service.requestProject(fixture.devOpsDeveloper().getId(), fixture.infrastructureTwo().getId()));
        });
    }

    @Test
    void cancellingIsBlockedOnceWorkHasAlreadyStarted() {
        withService(service -> {
            Fixture fixture = Fixture.bootstrap(service);

            ProjectRequest request = service.requestProject(fixture.frontendDeveloper().getId(), fixture.infinityProject().getId());
            service.acceptRequest(fixture.appLead().id(), fixture.infinityProject().getId(), request.id());
            service.startProject(fixture.frontendDeveloper().getId(), fixture.infinityProject().getId());

            assertThrows(IllegalStateException.class, () ->
                    service.cancelProject(fixture.appLead().id(), fixture.infinityProject().getId()));
        });
    }

    @Test
    void delayedCancellationClearsStaleProject() {
        withService(service -> {
            Fixture fixture = Fixture.bootstrap(service);
            Project idle = service.registerProject(fixture.appLead().id(), "Idle Project", Set.of(ProjectCategory.BACKEND), Duration.ofMillis(150));

            pause(300);

            assertEquals("CANCELED", service.getProjectDetails(idle.getId()).get("status"));
            assertFalse(service.getAvailableProjects().contains(idle));
        });
    }

    @Test
    void lifecycleRejectsPrematureCompletionBeforeAssignment() {
        withService(service -> {
            Fixture fixture = Fixture.bootstrap(service);

            ProjectRequest request = service.requestProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId());

            assertThrows(IllegalStateException.class, () ->
                    service.completeProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId()));
            assertThrows(IllegalStateException.class, () ->
                    service.startProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId()));

            service.acceptRequest(fixture.appLead().id(), fixture.infinityProject().getId(), request.id());

            assertThrows(IllegalStateException.class, () ->
                    service.completeProject(fixture.backendDeveloper().getId(), fixture.infinityProject().getId()));
        });
    }

    private static void withService(ServiceConsumer consumer) {
        try (ProjectTrackerService service = new ProjectTrackerService()) {
            consumer.accept(service);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for auto-cancel", ex);
        }
    }

    private interface ServiceConsumer {
        void accept(ProjectTrackerService service);
    }

    private record Fixture(Lead appLead,
                           Lead infraLead,
                           Developer frontendDeveloper,
                           Developer backendDeveloper,
                           Developer devOpsDeveloper,
                           Project infinityProject,
                           Project projectEuler,
                           Project infrastructureTwo) {

        static Fixture bootstrap(ProjectTrackerService service) {
            Lead appLead = service.registerLead("Lead A");
            Lead infraLead = service.registerLead("Lead B");
            Developer frontend = service.registerDeveloper("Frontend Dev", Set.of(ProjectCategory.FRONTEND));
            Developer backend = service.registerDeveloper("Backend Dev", Set.of(ProjectCategory.BACKEND));
            Developer devOps = service.registerDeveloper("DevOps Dev", Set.of(ProjectCategory.DEVOPS));
            Project infinityProject = service.registerProject(appLead.id(), "Infinity", Set.of(ProjectCategory.FRONTEND), Duration.ZERO);
            Project projectEuler = service.registerProject(infraLead.id(), "Project Euler", Set.of(ProjectCategory.DEVOPS), Duration.ZERO);
            Project infrastructureTwo = service.registerProject(infraLead.id(), "Infra 2", Set.of(ProjectCategory.DEVOPS), Duration.ZERO);
            return new Fixture(appLead, infraLead, frontend, backend, devOps, infinityProject, projectEuler, infrastructureTwo);
        }
    }
}
