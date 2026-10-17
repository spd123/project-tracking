package org.flipkart.notification;

import org.flipkart.model.Project;

public interface NotificationListener {
    void onProjectStatusChanged(Project project, String message);
}
