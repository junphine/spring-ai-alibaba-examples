package org.springframework.ai.mcp.sample.server;

import io.modelcontextprotocol.server.McpServerFeatures;
import static io.modelcontextprotocol.spec.McpSchema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MCPResourceConfig {

    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> myPrompts() {
        var prompt = new Prompt("er_diagram", "The assistants goal is to use the MCP server to create a visual ER diagram of the database.",
                List.of(new PromptArgument("name", "The name to database", true)));

        var promptSpecification = new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, getPromptRequest) -> {
            String nameArgument = (String) getPromptRequest.arguments().get("name");
            if (nameArgument == null) { nameArgument = "demo"; }
            var userMessage = new PromptMessage(Role.USER, new TextContent("Hello " + nameArgument + "! How can I assist you today?"));
            return new GetPromptResult("Visualize ER diagram", List.of(userMessage));
        });

        return List.of(promptSpecification);
    }
}
