package org.flipkart.notification;

import org.flipkart.model.Project;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LocalNotificationListener
        implements NotificationListener {
    private final List<String> notifications = new CopyOnWriteArrayList<>();

    @Override
    public void onProjectStatusChanged(Project project, String message) {
        notifications.add(project.getId() + " | " + message);
    }

    public List<String> getNotifications() {
        return List.copyOf(notifications);
    }
}
