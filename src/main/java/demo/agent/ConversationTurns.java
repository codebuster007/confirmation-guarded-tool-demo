package demo.agent;

import com.embabel.agent.api.common.OperationContext;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;

/**
 * Precondition shared by both agents: respond only when the last message is the user's.
 *
 * <p>This is deliberately not a {@code trigger = UserMessage.class}. In pause mode the
 * process resumes after the UI resolves the awaitable, and at that point the last object
 * on the blackboard is the verdict, not the user's message. The conversation still ends
 * with the user's message, so this condition holds and the action runs again.
 */
final class ConversationTurns {

    private ConversationTurns() {
    }

    static boolean isUsersTurn(OperationContext context) {
        Conversation conversation = context.getAgentProcess().last(Conversation.class);
        if (conversation == null) {
            return false;
        }
        var messages = conversation.getMessages();
        return !messages.isEmpty() && messages.get(messages.size() - 1) instanceof UserMessage;
    }
}
