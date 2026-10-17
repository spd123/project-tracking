package org.flipkart.strategy;

import org.flipkart.model.Developer;

import java.util.Comparator;

public final class RatingBasedOnRatingGivenByLeadStrategy implements DeveloperRatingStrategy {
    @Override
    public Comparator<Developer> comparator() {
        return Comparator.comparingDouble(Developer::getAverageRating).reversed()
                .thenComparing(Developer::getName);
    }
}
