package student40006741.recommendation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import student40006741.recommendation.dto.AiStructuredResponseDto;
import student40006741.recommendation.exception.RecommendationException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OpenAiRecommendationClient implements RecommendationAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiRecommendationClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = readConfig("openai.api.key", "OPENAI_API_KEY");
        this.model = readConfig("openai.model", "OPENAI_MODEL");
        this.baseUrl = readConfig("openai.base.url", "OPENAI_BASE_URL");
    }

    @Override
    public AiStructuredResponseDto analyzeText(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RecommendationException("OPENAI_API_KEY manquante");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(text), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new RecommendationException("Erreur OpenAI HTTP " + response.statusCode() + " : " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractContent(root);
            return objectMapper.readValue(cleanJson(content), AiStructuredResponseDto.class);
        } catch (IOException e) {
            throw new RecommendationException("Réponse IA invalide", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecommendationException("Appel OpenAI interrompu", e);
        }
    }

    private String buildRequestBody(String text) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");
        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "subscription_recommendation");
        jsonSchema.put("strict", true);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("mainNeed", stringSchema());
        properties.set("needLevel", enumSchema("low", "medium", "high"));
        properties.set("topDomain", enumSchema(
                "music",
                "video",
                "language_learning",
                "books",
                "news",
                "other"
        ));
        properties.set("confidence", numberSchema());

        ObjectNode interestsSchema = objectMapper.createObjectNode();
        interestsSchema.put("type", "array");
        interestsSchema.set("items", stringSchema());
        properties.set("interests", interestsSchema);

        ObjectNode recommendedTypeSchema = objectMapper.createObjectNode();
        recommendedTypeSchema.put("type", "object");
        ObjectNode recommendedTypeProperties = objectMapper.createObjectNode();
        recommendedTypeProperties.set("domain", enumSchema(
                "music",
                "video",
                "language_learning",
                "books",
                "news",
                "other"
        ));
        recommendedTypeProperties.set("utilityLevel", enumSchema("low", "medium", "high"));
        recommendedTypeProperties.set("reason", stringSchema());
        recommendedTypeSchema.set("properties", recommendedTypeProperties);
        recommendedTypeSchema.set("required", arrayOf("domain", "utilityLevel", "reason"));
        recommendedTypeSchema.put("additionalProperties", false);

        ObjectNode recommendedTypesSchema = objectMapper.createObjectNode();
        recommendedTypesSchema.put("type", "array");
        recommendedTypesSchema.set("items", recommendedTypeSchema);
        properties.set("recommendedTypes", recommendedTypesSchema);

        schema.set("properties", properties);
        schema.set("required", arrayOf(
                "mainNeed",
                "needLevel",
                "topDomain",
                "confidence",
                "interests",
                "recommendedTypes"
        ));
        schema.put("additionalProperties", false);

        jsonSchema.set("schema", schema);
        responseFormat.set("json_schema", jsonSchema);
        root.set("response_format", responseFormat);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message("system", """
                Tu analyses un texte utilisateur pour recommander des types d'abonnements.
                Réponds uniquement avec un JSON strictement conforme au schema fourni.

                Regles obligatoires :
                - Ecris tout le JSON en anglais pour les cles et les valeurs enumerees.
                - N'utilise jamais de domaines en francais comme "musique", "films", "culture", "apprentissage".
                - Tu dois choisir uniquement parmi ces domaines exacts : music, video, language_learning, books, news, other.
                - topDomain doit etre l'un de ces domaines exacts.
                - Chaque objet de recommendedTypes doit utiliser l'un de ces domaines exacts.
                - needLevel et utilityLevel doivent etre uniquement low, medium ou high.
                - confidence doit etre un nombre entre 0.0 et 1.0.
                - mainNeed peut etre en francais si utile, mais les enums doivent rester exactement celles imposees.
                - Si le texte parle surtout de musique, choisis music.
                - Si le texte parle de films, series, streaming video ou cinema, choisis video.
                - Si le texte parle d'apprentissage de langues, choisis language_learning.
                - Si aucun domaine ne correspond clairement, choisis other.
                """));
        messages.add(message("user", "Texte utilisateur: " + text));
        root.set("messages", messages);
        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private ObjectNode stringSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "string");
        return schema;
    }

    private ObjectNode numberSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "number");
        return schema;
    }

    private ObjectNode enumSchema(String... values) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "string");
        schema.set("enum", arrayOf(values));
        return schema;
    }

    private ArrayNode arrayOf(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private String extractContent(JsonNode root) {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new RecommendationException("Contenu IA introuvable dans la réponse");
        }
        return contentNode.asText();
    }

    private String cleanJson(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String readConfig(String propertyName, String envName) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        if ("openai.model".equals(propertyName)) {
            return "gpt-5.1";
        }
        if ("openai.base.url".equals(propertyName)) {
            return "https://api.openai.com/v1/chat/completions";
        }
        return null;
    }
}
