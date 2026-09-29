package demo.tasks;

import java.time.Instant;

/**
 * A task on the board.
 */
public record Task(String id, String title, int priority, Instant createdAt) {
}
