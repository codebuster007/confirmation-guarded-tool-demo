package demo.web;

import com.embabel.agent.api.channel.MessageOutputChannelEvent;
import com.embabel.agent.api.channel.OutputChannel;
import com.embabel.agent.api.channel.OutputChannelEvent;
import com.embabel.chat.AssistantMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Output channel for one chat session. Collects the assistant messages the agent sends
 * during a turn so the HTTP response can return them.
 */
final class CapturingOutputChannel implements OutputChannel {

    private final List<String> replies = new ArrayList<>();

    @Override
    public synchronized void send(OutputChannelEvent event) {
        if (event instanceof MessageOutputChannelEvent m && m.getMessage() instanceof AssistantMessage am) {
            replies.add(am.getContent());
        }
    }

    /** Replies captured since the last {@link #reset()}. */
    synchronized List<String> replies() {
        return List.copyOf(replies);
    }

    synchronized void reset() {
        replies.clear();
    }
}
