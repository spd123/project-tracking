package org.flipkart.strategy;

import org.flipkart.model.Developer;

import java.util.Comparator;

public final class ProjectsBasedRatingStrategy implements DeveloperRatingStrategy {
    @Override
    public Comparator<Developer> comparator() {
        return Comparator.comparingInt(Developer::getCompletedProjects).reversed()
                .thenComparing(Developer::getName);
    }
}
