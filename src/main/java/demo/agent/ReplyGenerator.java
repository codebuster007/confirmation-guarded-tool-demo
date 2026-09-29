package demo.agent;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.tool.ToolControlFlowSignal;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Conversation;
import demo.tools.TaskToolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The one LLM call both agents make: system prompt plus guard state, the mode's tools,
 * the conversation so far.
 *
 * <p>Gemini Flash occasionally returns an empty message, which the framework rejects.
 * One retry covers that; after that the user gets a fallback and the process stays alive.
 */
@Component
public class ReplyGenerator {

    static final String EMPTY_REPLY_FALLBACK =
            "Sorry, I did not get a usable answer from the model. Please say that again.";

    private static final int ATTEMPTS = 2;

    private static final Logger logger = LoggerFactory.getLogger(ReplyGenerator.class);

    private final TaskToolkit toolkit;

    public ReplyGenerator(TaskToolkit toolkit) {
        this.toolkit = toolkit;
    }

    public AssistantMessage reply(
            ActionContext context,
            Conversation conversation,
            DemoMode mode,
            String systemPrompt
    ) {
        String prompt = systemPrompt + ConfirmationPromptHints.forProcess(context.getAgentProcess(), mode);
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                return context.ai()
                        .withAutoLlm()
                        .withSystemPrompt(prompt)
                        .withTools(toolkit.forMode(mode))
                        .respond(conversation.getMessages());
            } catch (RuntimeException e) {
                if (e instanceof ToolControlFlowSignal) {
                    // AwaitableResponseException and friends: the framework handles these
                    throw e;
                }
                logger.warn("Model reply failed on attempt {} of {}: {}", attempt, ATTEMPTS, e.getMessage());
            }
        }
        return new AssistantMessage(EMPTY_REPLY_FALLBACK);
    }
}
