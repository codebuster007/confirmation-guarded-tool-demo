package demo.tasks;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory task store. Lives for the life of the JVM and is shared by both demo pages.
 */
@Component
public class TaskBoard {

    public static final int DEFAULT_PRIORITY = 2;

    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicBoolean failNextAdd = new AtomicBoolean();

    /**
     * Make the next {@link #add} throw once, to show how the guard behaves when the
     * confirmed tool fails: the proposal stays accepted and the model can retry.
     */
    public void failNextAdd() {
        failNextAdd.set(true);
    }

    public Task add(String title, Integer priority) {
        if (failNextAdd.compareAndSet(true, false)) {
            throw new IllegalStateException("Task board temporarily unavailable, please retry");
        }
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
        failNextAdd.set(false);
    }
}
