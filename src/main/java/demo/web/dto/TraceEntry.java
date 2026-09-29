package demo.web.dto;

import com.embabel.agent.api.tool.hitl.ToolCallOutcome;
import com.embabel.agent.api.tool.hitl.ToolCallProposal;
import com.embabel.agent.api.tool.hitl.ToolCallVerdict;

/**
 * One blackboard record of the guard, flattened for the page. {@code kind} says which
 * fields are populated: proposal, verdict or outcome.
 */
public record TraceEntry(
        String kind,
        String proposalId,
        String tool,
        String arguments,
        String message,
        Boolean accepted,
        String source,
        String note,
        Boolean executed,
        String timestamp
) {

    public static TraceEntry proposal(ToolCallProposal p) {
        return new TraceEntry("proposal", p.getId(), p.getToolName(), p.getArguments(), p.getMessage(),
                null, null, null, null, p.getTimestamp().toString());
    }

    public static TraceEntry verdict(ToolCallVerdict v) {
        return new TraceEntry("verdict", v.getProposalId(), null, null, null,
                v.getAccepted(), v.getSource().name(), v.getNote(), null, v.getTimestamp().toString());
    }

    public static TraceEntry outcome(ToolCallOutcome oc) {
        return new TraceEntry("outcome", oc.getProposalId(), null, null, null,
                null, null, null, oc.getExecuted(), oc.getTimestamp().toString());
    }
}
