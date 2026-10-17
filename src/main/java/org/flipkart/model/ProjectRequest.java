package org.flipkart.model;

import java.time.Instant;

public record ProjectRequest(String id, String projectId, String developerId, Instant createdAt) {
}
