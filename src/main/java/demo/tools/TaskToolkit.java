package demo.tools;

import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.hitl.ConfirmationGuardedTool;
import com.embabel.agent.api.tool.hitl.LlmConfirmation;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import demo.agent.DemoMode;
import demo.tasks.Task;
import demo.tasks.TaskBoard;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The tools the chat agents expose to the model.
 *
 * <p>{@code create_task} is a plain typed tool with a side effect. It is wrapped once per
 * {@link DemoMode} with {@link LlmConfirmation#guard}, so the only difference between the
 * two pages is the {@code ConfirmationMode} and {@code ConfirmationGuardOptions} the mode
 * supplies. {@code list_tasks} is not guarded.
 */
@Component
public class TaskToolkit {

    public record TaskRequest(
            @JsonPropertyDescription("Short task title") String title,
            @JsonPropertyDescription("Priority: 1 high, 2 normal, 3 low") Integer priority
    ) {
    }

    public record TaskResponse(String id, String title, int priority) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<DemoMode, List<Tool>> toolsByMode = new EnumMap<>(DemoMode.class);

    public TaskToolkit(TaskBoard board) {
        Tool createTask = Tool.fromFunction(
                "create_task",
                "Create a task on the task board. Returns the created task with its id.",
                TaskRequest.class,
                TaskResponse.class,
                request -> {
                    Task task = board.add(request.title(), request.priority());
                    return new TaskResponse(task.id(), task.title(), task.priority());
                }
        );

        Tool listTasks = Tool.create(
                "list_tasks",
                "List all tasks currently on the task board.",
                input -> Tool.Result.text(describe(board.all()))
        );

        for (DemoMode mode : DemoMode.values()) {
            ConfirmationGuardedTool guarded = LlmConfirmation.guard(
                    createTask,
                    this::confirmationMessage,
                    mode.confirmationMode(),
                    mode.guardOptions()
            );
            var tools = new ArrayList<>(guarded.tools()); // the guard and its verdict tool
            tools.add(listTasks);
            toolsByMode.put(mode, List.copyOf(tools));
        }
    }

    /** All tools for a mode: the guarded {@code create_task}, its verdict tool and {@code list_tasks}. */
    public List<Tool> forMode(DemoMode mode) {
        return toolsByMode.get(mode);
    }

    /** Human-facing confirmation text, built from the raw tool input. */
    private String confirmationMessage(String rawInput) {
        try {
            TaskRequest request = objectMapper.readValue(rawInput, TaskRequest.class);
            int priority = request.priority() == null ? TaskBoard.DEFAULT_PRIORITY : request.priority();
            return "Create task \"" + request.title() + "\" with priority " + priority + "?";
        } catch (Exception e) {
            return "Create this task: " + rawInput + "?";
        }
    }

    private static String describe(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "The board is empty.";
        }
        return tasks.stream()
                .map(t -> t.id() + " [P" + t.priority() + "] " + t.title())
                .collect(Collectors.joining("\n"));
    }
}
