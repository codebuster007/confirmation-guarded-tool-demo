package demo.web.dto;

import demo.tasks.Task;

import java.util.List;

/**
 * Everything the page needs after a turn: the assistant's replies, the task board, the
 * confirmation trail, and the request being awaited if the process paused.
 */
public record ChatResponse(
        List<String> replies,
        List<Task> tasks,
        List<TraceEntry> trace,
        Awaiting awaiting,
        String processStatus,
        String processId
) {

    public static ChatResponse empty(List<Task> tasks) {
        return new ChatResponse(List.of(), tasks, List.of(), null, null, null);
    }
}
