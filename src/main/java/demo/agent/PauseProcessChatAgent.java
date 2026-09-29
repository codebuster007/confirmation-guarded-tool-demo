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
 * Agent for {@link DemoMode#PAUSE}: when the guard raises a {@code ToolCallConfirmationRequest}
 * the process enters WAITING. The web layer resolves it through {@code onResponse} and runs
 * the process again; this action then reruns because the user's message is still the last
 * one in the conversation.
 */
@Agent(description = "Task board assistant; confirmations pause the process (PAUSE_PROCESS)")
public class PauseProcessChatAgent {

    static final String SYSTEM_PROMPT = """
            You are a task board assistant. You help the user create and list tasks.
            Rules:
            - When the user asks for a task, ALWAYS call create_task with the details.
              Assume priority 2 unless told otherwise. Never ask for confirmation yourself;
              the app asks the user to approve guarded tools before they take effect.
            - If the guarded tool state below says a call was approved or declined, follow
              that instruction exactly, then tell the user the result in one sentence.
            - Always reply with some text. Keep replies brief.
            """;

    private final ReplyGenerator replies;

    public PauseProcessChatAgent(ReplyGenerator replies) {
        this.replies = replies;
    }

    @Condition(name = "userTurn")
    public boolean userTurn(OperationContext context) {
        return ConversationTurns.isUsersTurn(context);
    }

    @Action(canRerun = true, pre = {"userTurn"})
    public ConversationStatus respond(Conversation conversation, ActionContext context) {
        var reply = replies.reply(context, conversation, DemoMode.PAUSE, SYSTEM_PROMPT);
        context.sendMessage(conversation.addMessage(reply));
        return new ConversationContinues(reply);
    }

    public Goal done() {
        return Goal.createInstance("Conversation is finished", ConversationOver.class, "done");
    }
}
