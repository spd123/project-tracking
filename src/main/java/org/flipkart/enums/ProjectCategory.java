package org.flipkart.enums;

public enum ProjectCategory {
    FRONTEND,
    BACKEND,
    DEVOPS;

    public static ProjectCategory fromLabel(String label) {
        return ProjectCategory.valueOf(label.trim().toUpperCase().replace('-', '_'));
    }
}
