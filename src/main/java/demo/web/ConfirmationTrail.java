package demo.web;

import com.embabel.agent.api.tool.hitl.ToolCallConfirmationRequest;
import com.embabel.agent.api.tool.hitl.ToolCallOutcome;
import com.embabel.agent.api.tool.hitl.ToolCallProposal;
import com.embabel.agent.api.tool.hitl.ToolCallVerdict;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import demo.web.dto.Awaiting;
import demo.web.dto.TraceEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the guard's records off a process blackboard for display. Nothing here changes state.
 */
final class ConfirmationTrail {

    private ConfirmationTrail() {
    }

    /** The proposal, verdict and outcome records, in the order they were written. */
    static List<TraceEntry> of(AgentProcess process) {
        var entries = new ArrayList<TraceEntry>();
        for (Object o : process.getBlackboard().getObjects()) {
            if (o instanceof ToolCallProposal p) {
                entries.add(TraceEntry.proposal(p));
            } else if (o instanceof ToolCallVerdict v) {
                entries.add(TraceEntry.verdict(v));
            } else if (o instanceof ToolCallOutcome oc) {
                entries.add(TraceEntry.outcome(oc));
            }
        }
        return entries;
    }

    /** The request the process is waiting on, when it is paused on a guarded tool. */
    static Optional<Awaiting> awaiting(AgentProcess process) {
        if (process.getStatus() == AgentProcessStatusCode.WAITING
                && process.lastResult() instanceof ToolCallConfirmationRequest request) {
            return Optional.of(Awaiting.of(request));
        }
        return Optional.empty();
    }
}
