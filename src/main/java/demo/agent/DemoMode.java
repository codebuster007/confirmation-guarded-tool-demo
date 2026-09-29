package demo.agent;

import com.embabel.agent.api.tool.hitl.ConfirmationGuardOptions;
import com.embabel.agent.api.tool.hitl.ConfirmationMode;

/**
 * The two demo pages. Each mode pairs a {@link ConfirmationMode} with the
 * {@link ConfirmationGuardOptions} used to build the guarded tool, and names the
 * agent that serves it. Everything else (tools, sessions, HTTP) is driven from here.
 */
public enum DemoMode {

    /** The LLM asks the user in chat and records the answer through the verdict tool. */
    LLM(
            "llm",
            AskViaLlmChatAgent.class.getSimpleName(),
            ConfirmationMode.ASK_VIA_LLM,
            ConfirmationGuardOptions.DEFAULT
    ),

    /** The process pauses; the app shows Approve / Decline and resolves the awaitable. */
    PAUSE(
            "pause",
            PauseProcessChatAgent.class.getSimpleName(),
            ConfirmationMode.PAUSE_PROCESS,
            ConfirmationGuardOptions.DEFAULT
                    .withConfirmationNote("The user approves this in the app before it takes effect.")
                    .withVerdictToolPrefix("approve_")
    );

    private final String path;
    private final String agentName;
    private final ConfirmationMode confirmationMode;
    private final ConfirmationGuardOptions guardOptions;

    DemoMode(String path, String agentName, ConfirmationMode confirmationMode, ConfirmationGuardOptions guardOptions) {
        this.path = path;
        this.agentName = agentName;
        this.confirmationMode = confirmationMode;
        this.guardOptions = guardOptions;
    }

    /** Segment used in URLs: {@code /api/{path}/...} and {@code /?mode={path}}. */
    public String path() {
        return path;
    }

    /** Name under which the agent platform registers the agent for this mode. */
    public String agentName() {
        return agentName;
    }

    public ConfirmationMode confirmationMode() {
        return confirmationMode;
    }

    public ConfirmationGuardOptions guardOptions() {
        return guardOptions;
    }

    public static DemoMode fromPath(String path) {
        for (DemoMode mode : values()) {
            if (mode.path.equals(path)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown mode '" + path + "'. Use 'llm' or 'pause'.");
    }
}
