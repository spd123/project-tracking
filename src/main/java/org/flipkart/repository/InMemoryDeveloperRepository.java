package org.flipkart.repository;

import org.flipkart.model.Developer;

public final class InMemoryDeveloperRepository extends AbstractInMemoryRepository<Developer> implements DeveloperRepository {
    @Override
    public void save(Developer developer) {
        save(developer.getId(), developer);
    }

    @Override
    public Developer findById(String developerId) {
        return lookup(developerId);
    }

    @Override
    public java.util.List<Developer> findAll() {
        return snapshot();
    }
}
