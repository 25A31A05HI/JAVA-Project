
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GiftPlanner {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8000")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", port), 0
        );

        server.createContext("/recommend", exchange -> {

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405,
                    "{\"error\":\"Only POST requests are allowed\"}");
                return;
            }

            try {
                String request = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                );

                String name = getJsonString(request, "name");
                String age = getJsonString(request, "age");
                String relationship = getJsonString(request, "relationship");
                String occasion = getJsonString(request, "occasion");
                String interests = getJsonString(request, "interests");
                String budget = getJsonString(request, "budget");

                if (name.isBlank() || age.isBlank()
                        || occasion.isBlank() || budget.isBlank()) {
                    sendResponse(exchange, 400,
                        "{\"error\":\"Please provide all required details\"}");
                    return;
                }

                String result = generateRecommendations(
                    name, age, relationship, occasion, interests, budget
                );

                sendResponse(exchange, 200, result);

            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400,
                    "{\"error\":\"Invalid JSON request\"}");

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 502,
                    "{\"error\":\"Unable to generate recommendations. Please try again.\"}");
            }
        });

        server.start();
        System.out.println("Gemini Gift Planner running on port " + port);
    }

    // Extracts a string value from JSON.
    private static String getJsonString(String json, String key) {

        String search = "\"" + key + "\"";
        int keyPosition = json.indexOf(search);

        if (keyPosition < 0) {
            return "";
        }

        int colon = json.indexOf(':', keyPosition + search.length());

        if (colon < 0) {
            throw new IllegalArgumentException("Missing colon");
        }

        int start = colon + 1;

        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length() || json.charAt(start) != '"') {
            return "";
        }

        start++;

        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {

            char c = json.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    default -> value.append(c);
                }
                escaped = false;

            } else if (c == '\\') {
                escaped = true;

            } else if (c == '"') {
                return value.toString();

            } else {
                value.append(c);
            }
        }

        throw new IllegalArgumentException("Unterminated JSON string");
    }

    // Generates personalized gift recommendations using Gemini.
    private static String generateRecommendations(
            String name,
            String age,
            String relationship,
            String occasion,
            String interests,
            String budget) throws Exception {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Gemini API key is missing from Render"
            );
        }

        String prompt = """
            You are an expert personalized gift recommendation assistant.

            Suggest exactly 7 thoughtful and creative gifts using these details:

            Recipient name: %s
            Age: %s
            Relationship: %s
            Occasion: %s
            Interests and hobbies: %s
            Budget: %s INR

            Instructions:
            1. Recommend gifts suitable for the recipient's age.
            2. Consider their interests and hobbies carefully.
            3. Respect the given budget.
            4. Make every recommendation different and practical.
            5. Avoid generic suggestions whenever possible.
            6. Give a short explanation for each gift.

            Return exactly 7 recommendations.
            Put each recommendation on a separate line.
            Do not include an introduction or conclusion.
            Use this format:
            Gift name - Short explanation
            """.formatted(
                name, age, relationship, occasion, interests, budget
            );

        String requestBody =
            "{\"contents\":[{\"parts\":[{\"text\":\""
            + escapeJson(prompt)
            + "\"}]}],\"generationConfig\":{\"temperature\":0.8,\"maxOutputTokens\":1200}}";

        int maxAttempts = 4;

        HttpResponse<String> response = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            if (status == 200) {
                break;
            }

            System.err.println(
                "Gemini API error: " + status
                + " (Attempt " + attempt + "/" + maxAttempts + ")"
                + " - " + response.body()
            );

            // Retry temporary errors only.
            if (status != 429 && status != 500
                    && status != 502 && status != 503
                    && status != 504) {
                throw new RuntimeException(
                    "Gemini API returned error: " + status
                );
            }

            if (attempt == maxAttempts) {
                throw new RuntimeException(
                    "Gemini is still unavailable after 4 attempts"
                );
            }

            // Wait 2, 4, then 8 seconds before retrying.
            long waitTime = 2000L * (1L << (attempt - 1));

            System.out.println(
                "Retrying Gemini in " + (waitTime / 1000) + " seconds..."
            );

            Thread.sleep(waitTime);
        }

        if (response == null || response.statusCode() != 200) {
            throw new RuntimeException("Gemini request failed");
        }

        String responseText = getJsonString(
            response.body(), "text"
        );

        if (responseText.isBlank()) {
            throw new RuntimeException(
                "Gemini returned an empty response"
            );
        }

        List<String> gifts = new ArrayList<>();

        for (String line : responseText.split("\\R")) {

            String gift = line.trim();

            gift = gift.replaceFirst("^[-*•]\\s*", "");
            gift = gift.replaceFirst("^\\d+[.)]\\s*", "");

            if (!gift.isBlank()) {
                gifts.add(gift);
            }

            if (gifts.size() == 7) {
                break;
            }
        }

        if (gifts.isEmpty()) {
            throw new RuntimeException(
                "No recommendations received from Gemini"
            );
        }

        StringBuilder json = new StringBuilder();

        json.append("{\"recipient\":\"")
            .append(escapeJson(name))
            .append("\",\"recommendations\":[");

        for (int i = 0; i < gifts.size(); i++) {

            if (i > 0) {
                json.append(",");
            }

            json.append("\"")
                .append(escapeJson(gifts.get(i)))
                .append("\"");
        }

        json.append("],\"message\":\"")
            .append(escapeJson(
                "Personalized AI gift recommendations generated by Gemini."
            ))
            .append("\"}");

        return json.toString();
    }

    // Escapes special characters before placing text inside JSON.
    private static String escapeJson(String value) {

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Sends JSON responses to the Streamlit frontend.
    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String response) throws IOException {

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
