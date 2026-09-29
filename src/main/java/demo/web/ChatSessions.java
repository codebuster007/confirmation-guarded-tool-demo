package demo.web;

import com.embabel.agent.api.common.PlannerType;
import com.embabel.agent.api.tool.hitl.ToolCallConfirmationRequest;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.Verbosity;
import com.embabel.agent.core.hitl.ConfirmationResponse;
import com.embabel.chat.ChatSession;
import com.embabel.chat.UserMessage;
import com.embabel.chat.agent.AgentProcessChatbot;
import com.embabel.chat.support.InMemoryConversationFactory;
import demo.agent.DemoMode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat sessions keyed by mode and browser session id. One {@link AgentProcessChatbot} per
 * mode, each backed by that mode's agent. Knows how to send a user message and, for pause
 * mode, how to resolve a waiting confirmation the way any UI would: {@code onResponse},
 * then run the process again.
 */
@Component
public class ChatSessions {

    /** A session together with its capturing channel and the process behind it. */
    public record Live(ChatSession session, CapturingOutputChannel channel, AgentProcess process) {

        public List<String> replies() {
            return channel.replies();
        }
    }

    private final AgentPlatform agentPlatform;
    private final Map<DemoMode, AgentProcessChatbot> chatbots = new EnumMap<>(DemoMode.class);
    private final Map<String, Live> sessions = new ConcurrentHashMap<>();

    public ChatSessions(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
        for (DemoMode mode : DemoMode.values()) {
            chatbots.put(mode, chatbotFor(mode));
        }
    }

    /** Send a user message and run the process until it replies, pauses or gets stuck. */
    public Live send(DemoMode mode, String sessionId, String text) {
        Live live = sessions.computeIfAbsent(key(mode, sessionId), k -> open(mode));
        synchronized (live) {
            live.channel().reset();
            live.session().onUserMessage(new UserMessage(text));
            return live;
        }
    }

    /**
     * Resolve the confirmation the process is waiting on (pause mode) and resume it.
     * The guard sees the resulting {@code ToolCallVerdict} on the next call.
     */
    public Live resolve(DemoMode mode, String sessionId, boolean accepted) {
        Live live = find(mode, sessionId)
                .orElseThrow(() -> new IllegalStateException("Unknown session " + sessionId));
        synchronized (live) {
            if (!(live.process().lastResult() instanceof ToolCallConfirmationRequest awaiting)) {
                throw new IllegalStateException("Nothing is awaiting confirmation");
            }
            var response = new ConfirmationResponse(
                    UUID.randomUUID().toString(), awaiting.getId(), accepted, false, Instant.now());
            awaiting.onResponse(response, live.process());
            live.channel().reset();
            live.process().run();
            return live;
        }
    }

    public Optional<Live> find(DemoMode mode, String sessionId) {
        return Optional.ofNullable(sessions.get(key(mode, sessionId)));
    }

    public void clear() {
        sessions.clear();
    }

    private Live open(DemoMode mode) {
        var channel = new CapturingOutputChannel();
        ChatSession session = chatbots.get(mode).createSession(null, channel, null, null);
        AgentProcess process = agentPlatform.getAgentProcess(session.getProcessId());
        return new Live(session, channel, process);
    }

    private AgentProcessChatbot chatbotFor(DemoMode mode) {
        return new AgentProcessChatbot(
                agentPlatform,
                user -> agentPlatform.agents().stream()
                        .filter(a -> a.getName().equals(mode.agentName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Agent " + mode.agentName() + " is not registered")),
                new InMemoryConversationFactory(),
                (user, channel) -> List.of(),
                PlannerType.GOAP,
                new Verbosity()
        );
    }

    private static String key(DemoMode mode, String sessionId) {
        return mode.path() + ":" + sessionId;
    }
}
