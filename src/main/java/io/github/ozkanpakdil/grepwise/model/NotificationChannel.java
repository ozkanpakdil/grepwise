package io.github.ozkanpakdil.grepwise.model;

import java.util.Objects;

/**
 * Represents a notification channel for alarms.
 */
public class NotificationChannel {
    private String type; // EMAIL, SLACK, WEBHOOK, etc.
    private String destination; // email address, slack channel, webhook URL, etc.

    public NotificationChannel() {
    }

    public NotificationChannel(String type, String destination) {
        this.type = type;
        this.destination = destination;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationChannel that = (NotificationChannel) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, destination);
    }

    @Override
    public String toString() {
        return "NotificationChannel{" +
                "type='" + type + '\'' +
                ", destination='" + destination + '\'' +
                '}';
    }
}