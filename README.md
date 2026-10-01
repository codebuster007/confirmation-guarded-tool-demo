# confirmation-guarded-tool-demo

https://github.com/user-attachments/assets/80ab97a5-2909-4810-9c4d-22249ede2f97

A small Spring Boot chat app that shows `ConfirmationGuardedTool` from
[embabel/embabel-agent#1550](https://github.com/embabel/embabel-agent/issues/1550) in action.
Tasks live in memory. Gemini is the model.

Two pages, one guarded tool (`create_task`), two ways of getting the human's confirmation:

| Page | Mode | Who collects the answer | Verdict tool |
|------|------|-------------------------|--------------|
| `http://localhost:8090/?mode=llm` (default) | `ConfirmationMode.ASK_VIA_LLM` | The model asks in chat and calls the verdict tool | `confirm_create_task` |
| `http://localhost:8090/?mode=pause` | `ConfirmationMode.PAUSE_PROCESS` | The process pauses; the app shows Approve / Decline and resolves it through `onResponse` | `approve_create_task` (custom `ConfirmationGuardOptions`) |

The right-hand side of each page shows the task board, the confirmation trail read
straight from the process blackboard (`ToolCallProposal` → `ToolCallVerdict` → `ToolCallOutcome`),
and the tool definitions the model sees. On the pause page the custom confirmation note
and verdict prefix are visible there.

## Try

ASK_VIA_LLM page:

1. "Add a task to write the release notes, high priority". The model calls `create_task`;
   the guard records a *pending* proposal and the model asks you to confirm. No task yet.
2. "yes". The model calls `confirm_create_task` with `accepted=true`. The task appears; the
   trail shows *executed*, verdict via LLM.
3. "Add a task to email the team" then "no". Trail shows *declined*, no task.

PAUSE_PROCESS page:

1. "Add a task to write the release notes, high priority". The guard throws a
   `ToolCallConfirmationRequest`, the process is `WAITING`, and an approval card appears.
2. Click **Approve**. The app calls `onResponse` (verdict via UI) and runs the process again.
   The model repeats the call, the guard executes the stored arguments, the task appears.
3. Ask for another task and click **Decline**. The trail shows *declined*, no task.

## Run

Prerequisites: JDK 21+, and the `1550-llm-confirmation` branch of embabel-agent installed
locally at `1.5.3-SNAPSHOT`, from the embabel-agent checkout:

```bash
mvn -o install -DskipTests -Dmaven.javadoc.skip=true -Dlicense.skip=true -pl embabel-agent-starters/embabel-agent-starter-gemini -am
```

Put your Gemini key in `application-local.properties` next to `pom.xml` (gitignored):

```properties
embabel.agent.platform.models.gemini.api-key=YOUR_KEY
```

or export `GEMINI_API_KEY`. Then:

```bash
mvn spring-boot:run
```

and open http://localhost:8090.

To use another provider, swap the starter dependency in `pom.xml` (for example
`embabel-agent-starter-openai` or `embabel-agent-starter-anthropic`), supply that
provider's key, and change `embabel.models.default-llm` in `application.properties`.

## Where to look

```
demo
├── tasks   Task, TaskBoard               the in-memory domain, nothing agent-specific
├── tools   TaskToolkit                   create_task and list_tasks; create_task is wrapped
│                                         once per DemoMode with ConfirmationGuardedTool.of(...)
├── agent   DemoMode                      the two pages: ConfirmationMode + ConfirmationGuardOptions + agent name
│           AskViaLlmChatAgent            @Agent for ASK_VIA_LLM
│           PauseProcessChatAgent         @Agent for PAUSE_PROCESS
│           ReplyGenerator                the one LLM call both agents make
│           ConfirmationPromptHints       blackboard records → system prompt section
│           ConversationTurns             "it is the user's turn" precondition
└── web     ChatController                HTTP only
            ChatSessions                  sessions per mode; send, and resolve (onResponse + run)
            CapturingOutputChannel        collects assistant replies for the HTTP response
            ConfirmationTrail             blackboard records → DTOs for the page
            dto/                          request and response records
```

The only framework-facing code is in `TaskToolkit` (building the guard), `ChatSessions.resolve`
(resolving a `ToolCallConfirmationRequest` through `onResponse`), and the two agents.
`ConfirmationPromptHints` exists because the tool loop's history is not part of the
conversation the model sees on the next turn.
