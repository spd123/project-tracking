package org.flipkart.util;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerationService {
    private static final AtomicLong leadIdSequence = new AtomicLong(1);
    private static final AtomicLong developerIdSequence = new AtomicLong(1);
    private static final AtomicLong projectIdSequence = new AtomicLong(1);
    private static final AtomicLong requestIdSequence = new AtomicLong(1);

    private IdGenerationService() {
    }

    public static String nextLeadId() {
        return "lead-" + leadIdSequence.getAndIncrement();
    }

    public static String nextDeveloperId() {
        return "developer-" + developerIdSequence.getAndIncrement();
    }

    public static String nextProjectId() {
        return "project-" + projectIdSequence.getAndIncrement();
    }

    public static String nextRequestId() {
        return "request-" + requestIdSequence.getAndIncrement();
    }
}
