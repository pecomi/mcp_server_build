package host.llm;

public record ToolDescriptor(
        String name,
        String title,
        String description,
        String inputSchemaJson
) {}
