package demo.web.dto;

import com.embabel.agent.api.tool.Tool;

/**
 * What the model sees for one tool: name, description and JSON schema.
 */
public record ToolInfo(String name, String description, String inputSchema) {

    public static ToolInfo of(Tool tool) {
        var definition = tool.getDefinition();
        return new ToolInfo(definition.getName(), definition.getDescription(), definition.getInputSchema().toJsonSchema());
    }
}
