package demo.web.dto;

import com.embabel.agent.api.tool.hitl.ToolCallConfirmationRequest;

/**
 * Present while the process is WAITING on a guarded tool (pause mode). The page renders
 * Approve / Decline for it.
 */
public record Awaiting(String requestId, String proposalId, String tool, String arguments, String message) {

    public static Awaiting of(ToolCallConfirmationRequest request) {
        var proposal = request.getPayload();
        return new Awaiting(request.getId(), proposal.getId(), proposal.getToolName(),
                proposal.getArguments(), proposal.getMessage());
    }
}
