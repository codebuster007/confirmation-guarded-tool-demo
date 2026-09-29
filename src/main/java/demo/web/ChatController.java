package demo.web;

import demo.agent.DemoMode;
import demo.tasks.TaskBoard;
import demo.tools.TaskToolkit;
import demo.web.dto.ChatRequest;
import demo.web.dto.ChatResponse;
import demo.web.dto.ResolveRequest;
import demo.web.dto.ToolInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HTTP API for both pages. {@code {mode}} is {@code llm} or {@code pause}, see {@link DemoMode}.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatSessions sessions;
    private final TaskBoard board;
    private final TaskToolkit toolkit;

    public ChatController(ChatSessions sessions, TaskBoard board, TaskToolkit toolkit) {
        this.sessions = sessions;
        this.board = board;
        this.toolkit = toolkit;
    }

    @PostMapping("/{mode}/chat")
    public ChatResponse chat(@PathVariable String mode, @RequestBody ChatRequest request) {
        return snapshot(sessions.send(DemoMode.fromPath(mode), request.sessionId(), request.message()));
    }

    /** Pause mode only: the human's Approve / Decline. */
    @PostMapping("/{mode}/resolve")
    public ChatResponse resolve(@PathVariable String mode, @RequestBody ResolveRequest request) {
        return snapshot(sessions.resolve(DemoMode.fromPath(mode), request.sessionId(), request.accepted()));
    }

    @GetMapping("/{mode}/state/{sessionId}")
    public ChatResponse state(@PathVariable String mode, @PathVariable String sessionId) {
        return sessions.find(DemoMode.fromPath(mode), sessionId)
                .map(this::snapshot)
                .orElseGet(() -> ChatResponse.empty(board.all()));
    }

    @GetMapping("/{mode}/tools")
    public List<ToolInfo> tools(@PathVariable String mode) {
        return toolkit.forMode(DemoMode.fromPath(mode)).stream().map(ToolInfo::of).toList();
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        sessions.clear();
        board.clear();
        return Map.of("status", "ok");
    }

    private ChatResponse snapshot(ChatSessions.Live live) {
        var process = live.process();
        return new ChatResponse(
                live.replies(),
                board.all(),
                ConfirmationTrail.of(process),
                ConfirmationTrail.awaiting(process).orElse(null),
                process.getStatus().name(),
                process.getId()
        );
    }
}
