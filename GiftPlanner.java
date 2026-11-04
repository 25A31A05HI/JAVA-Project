import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class GiftPlanner {

    public static void main(String[] args) throws IOException {
        // Hosting platforms provide the port through the PORT environment variable.
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
                sendResponse(exchange, 500,
                    "{\"error\":\"An internal server error occurred\"}");
            }
        });

        server.start();
        System.out.println("Gift Planner backend running on port " + port);
    }

    // Reads a string value from the simple JSON request sent by app.py.
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

    // Initial placeholder recommendations; Gemini AI will be integrated later.
    private static String generateRecommendations(
            String name,
            String age,
            String relationship,
            String occasion,
            String interests,
            String budget) {

        String[] gifts = {
            "Personalized Gift Box",
            "Customized Photo Frame",
            "Smart Watch",
            "Premium Book Collection",
            "Personalized Accessories",
            "Wireless Earbuds",
            "Handmade Memory Scrapbook"
        };

        StringBuilder json = new StringBuilder();
        json.append("{\"recipient\":\"")
            .append(escapeJson(name))
            .append("\",\"recommendations\":[");

        for (int i = 0; i < gifts.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(gifts[i])).append("\"");
        }

        json.append("],\"message\":\"")
            .append(escapeJson(
                "Suggestions prepared for " + name
                + " (age " + age + "), relationship: " + relationship
                + ", occasion: " + occasion
                + ", interests: " + interests
                + ", budget: " + budget
            ))
            .append("\"}");

        return json.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static void sendResponse(
            HttpExchange exchange, int status, String response)
            throws IOException {

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
