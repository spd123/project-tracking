package org.flipkart.repository;

import org.flipkart.model.Lead;

import java.util.List;

public interface LeadRepository {
    void save(Lead lead);

    Lead findById(String leadId);

    List<Lead> findAll();
}
