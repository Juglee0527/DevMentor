package com.devmentor.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class AiResponseSchemaFactory {

    private final ObjectMapper objectMapper;

    public AiResponseSchemaFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode tutorSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("answer", stringSchema());
        properties.set("detectedConcepts", arraySchema(detectedConceptSchema()));
        properties.set("knowledgeGaps", arraySchema(knowledgeGapSchema()));
        properties.set("followUpQuestion", nullableStringSchema());
        properties.set("recommendedConcepts", arraySchema(recommendedConceptSchema()));
        schema.set("required", stringArray(
                "answer",
                "detectedConcepts",
                "knowledgeGaps",
                "followUpQuestion",
                "recommendedConcepts"
        ));
        return schema;
    }

    public JsonNode assessmentSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("correct", objectMapper.createObjectNode().put("type", "boolean"));
        properties.set("score", objectMapper.createObjectNode()
                .put("type", "integer")
                .put("minimum", 0)
                .put("maximum", 100));
        properties.set("feedback", stringSchema());
        properties.set("correctAnswer", stringSchema());
        properties.set("reviewRequired", objectMapper.createObjectNode().put("type", "boolean"));
        schema.set("required", stringArray(
                "correct",
                "score",
                "feedback",
                "correctAnswer",
                "reviewRequired"
        ));
        return schema;
    }

    private ObjectNode detectedConceptSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("skillCode", stringSchema());
        properties.set("conceptCode", stringSchema());
        properties.set("confidence", objectMapper.createObjectNode()
                .put("type", "number")
                .put("minimum", 0)
                .put("maximum", 1));
        schema.set("required", stringArray("skillCode", "conceptCode", "confidence"));
        return schema;
    }

    private ObjectNode knowledgeGapSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("skillCode", stringSchema());
        properties.set("conceptCode", stringSchema());
        properties.set("reason", stringSchema());
        schema.set("required", stringArray("skillCode", "conceptCode", "reason"));
        return schema;
    }

    private ObjectNode recommendedConceptSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("skillCode", stringSchema());
        properties.set("conceptCode", stringSchema());
        properties.set("reason", stringSchema());
        schema.set("required", stringArray("skillCode", "conceptCode", "reason"));
        return schema;
    }

    private ObjectNode objectSchema() {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .put("additionalProperties", false);
    }

    private ObjectNode stringSchema() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode nullableStringSchema() {
        ArrayNode types = objectMapper.createArrayNode().add("string").add("null");
        return objectMapper.createObjectNode().set("type", types);
    }

    private ObjectNode arraySchema(JsonNode itemSchema) {
        return objectMapper.createObjectNode()
                .put("type", "array")
                .set("items", itemSchema);
    }

    private ArrayNode stringArray(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
