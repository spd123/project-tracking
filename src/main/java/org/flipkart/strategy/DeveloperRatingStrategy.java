package org.flipkart.strategy;

import org.flipkart.model.Developer;

import java.util.Comparator;

public interface DeveloperRatingStrategy {
    Comparator<Developer> comparator();
}
