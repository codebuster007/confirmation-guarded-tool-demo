package demo.agent;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.Goal;
import com.embabel.chat.Conversation;
import com.embabel.chat.agent.ConversationContinues;
import com.embabel.chat.agent.ConversationOver;
import com.embabel.chat.agent.ConversationStatus;

/**
 * Agent for {@link DemoMode#LLM}: the model collects the user's confirmation in chat and
 * records it through {@code confirm_create_task}. The process never pauses.
 *
 * <p>The whole conversation runs inside one AgentProcess, so the records the guard writes
 * to the blackboard survive between turns.
 */
@Agent(description = "Task board assistant; the model asks for confirmation (ASK_VIA_LLM)")
public class AskViaLlmChatAgent {

    static final String SYSTEM_PROMPT = """
            You are a task board assistant. You help the user create and list tasks.
            Rules:
            - When the user asks for a task, ALWAYS call create_task with the details.
              Assume priority 2 unless told otherwise. Never ask for confirmation yourself;
              only the tool decides whether confirmation is needed.
            - When a tool result says a confirmation is required, relay its question to the
              user in one short sentence and stop. Do not assume consent.
            - When the user answers a pending confirmation, call the matching confirm_ tool
              with accepted=true or accepted=false, then tell the user what happened in one sentence.
            - Always reply with some text. Keep replies brief.
            """;

    private final ReplyGenerator replies;

    public AskViaLlmChatAgent(ReplyGenerator replies) {
        this.replies = replies;
    }

    @Condition(name = "userTurn")
    public boolean userTurn(OperationContext context) {
        return ConversationTurns.isUsersTurn(context);
    }

    @Action(canRerun = true, pre = {"userTurn"})
    public ConversationStatus respond(Conversation conversation, ActionContext context) {
        var reply = replies.reply(context, conversation, DemoMode.LLM, SYSTEM_PROMPT);
        context.sendMessage(conversation.addMessage(reply));
        // Never reaches the goal on purpose: the process stays alive for the next user message.
        return new ConversationContinues(reply);
    }

    /** Goal getter, picked up by the agent metadata reader. Same shape as DefaultChatAgentBuilder. */
    public Goal done() {
        return Goal.createInstance("Conversation is finished", ConversationOver.class, "done");
    }
}
