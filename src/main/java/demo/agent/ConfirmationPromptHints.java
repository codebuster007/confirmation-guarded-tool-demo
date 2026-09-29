package demo.agent;

import com.embabel.agent.api.tool.hitl.ToolCallOutcome;
import com.embabel.agent.api.tool.hitl.ToolCallProposal;
import com.embabel.agent.api.tool.hitl.ToolCallVerdict;
import com.embabel.agent.core.AgentProcess;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns the guard's blackboard records into a short system-prompt section.
 *
 * <p>The tool loop's own history (tool calls and their results) is not part of the
 * {@code Conversation} the model sees on the next turn. Without this, the model would not
 * know that a proposal is pending, or that the user approved one through the UI while the
 * process was paused.
 */
final class ConfirmationPromptHints {

    private ConfirmationPromptHints() {
    }

    static String forProcess(AgentProcess process, DemoMode mode) {
        var objects = process.getBlackboard().getObjects();
        Map<String, ToolCallVerdict> verdicts = new HashMap<>();
        Map<String, ToolCallOutcome> outcomes = new HashMap<>();
        Map<String, ToolCallProposal> latestPerTool = new LinkedHashMap<>();
        for (Object o : objects) {
            if (o instanceof ToolCallProposal p) latestPerTool.put(p.getToolName(), p);
            if (o instanceof ToolCallVerdict v) verdicts.put(v.getProposalId(), v);
            if (o instanceof ToolCallOutcome oc) outcomes.put(oc.getProposalId(), oc);
        }

        var sb = new StringBuilder();
        for (ToolCallProposal p : latestPerTool.values()) {
            if (outcomes.containsKey(p.getId())) {
                continue; // consumed: nothing to say
            }
            ToolCallVerdict verdict = verdicts.get(p.getId());
            if (verdict == null) {
                sb.append(pending(p, mode.guardOptions().getVerdictToolPrefix()));
            } else if (verdict.getAccepted()) {
                sb.append(approved(p));
            } else {
                sb.append(declined(p));
            }
        }
        return sb.isEmpty() ? "" : "\n\nGuarded tool state:\n" + sb;
    }

    private static String pending(ToolCallProposal p, String verdictToolPrefix) {
        return "- Pending: " + p.getToolName() + " with arguments " + p.getArguments()
                + ". Question asked: \"" + p.getMessage() + "\". If the user's latest message answers it, call "
                + verdictToolPrefix + p.getToolName() + " now.\n";
    }

    private static String approved(ToolCallProposal p) {
        return "- Approved by the user: " + p.getToolName() + " with arguments " + p.getArguments()
                + ". Call " + p.getToolName() + " again with exactly these arguments to execute it, then tell the user.\n";
    }

    private static String declined(ToolCallProposal p) {
        return "- Declined by the user: " + p.getToolName() + " with arguments " + p.getArguments()
                + ". Call " + p.getToolName() + " once more with exactly these arguments so the decline is recorded, "
                + "then tell the user it was not done. Do not propose it again.\n";
    }
}
