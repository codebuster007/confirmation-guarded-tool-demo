package demo.tasks;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory task store. Lives for the life of the JVM and is shared by both demo pages.
 */
@Component
public class TaskBoard {

    public static final int DEFAULT_PRIORITY = 2;

    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger();

    public Task add(String title, Integer priority) {
        var task = new Task(
                "T-" + counter.incrementAndGet(),
                title,
                priority == null ? DEFAULT_PRIORITY : priority,
                Instant.now()
        );
        tasks.add(task);
        return task;
    }

    public List<Task> all() {
        return List.copyOf(tasks);
    }

    public void clear() {
        tasks.clear();
        counter.set(0);
    }
}
