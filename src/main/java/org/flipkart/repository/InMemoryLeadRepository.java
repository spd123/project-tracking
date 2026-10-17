package org.flipkart.repository;

import org.flipkart.model.Lead;

public final class InMemoryLeadRepository extends AbstractInMemoryRepository<Lead> implements LeadRepository {
    @Override
    public void save(Lead lead) {
        save(lead.id(), lead);
    }

    @Override
    public Lead findById(String leadId) {
        return lookup(leadId);
    }

    @Override
    public java.util.List<Lead> findAll() {
        return snapshot();
    }
}
