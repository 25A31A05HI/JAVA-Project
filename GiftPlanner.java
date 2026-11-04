
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GiftPlanner {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8000), 0
        );

        server.createContext("/recommend", exchange -> {

            if (!exchange.getRequestMethod().equals("POST")) {
                sendResponse(exchange, 405,
                    "{\"error\":\"Only POST requests allowed\"}");
                return;
            }

            try {
                String request = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                );

                String name = getValue(request, "name");
                String age = getValue(request, "age");
                String relationship = getValue(request, "relationship");
                String occasion = getValue(request, "occasion");
                String interests = getValue(request, "interests");
                String budget = getValue(request, "budget");

                if (name.isBlank() || age.isBlank()
                    || occasion.isBlank() || budget.isBlank()) {

                    sendResponse(exchange, 400,
                        "{\"error\":\"Please provide all required details\"}");
                    return;
                }

                String result = generateRecommendations(
                    name, age, relationship,
                    occasion, interests, budget
                );

                sendResponse(exchange, 200, result);

            } catch (Exception e) {
                sendResponse(exchange, 400,
                    "{\"error\":\"Invalid request\"}");
            }
        });

        server.start();

        System.out.println(
            "AI Gift Planner backend running on port 8000"
        );
    }

    // Extract values from incoming JSON
    private static String getValue(String json, String key) {

        String search = "\"" + key + "\"";
        int start = json.indexOf(search);

        if (start == -1) return "";

        start = json.indexOf(":", start) + 1;

        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length() || json.charAt(start) != '"')
            return "";

        start++;

        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {

            char c = json.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    default: value.append(c);
                }
                escaped = false;
            }
            else if (c == '\\') {
                escaped = true;
            }
            else if (c == '"') {
                return value.toString();
            }
            else {
                value.append(c);
            }
        }

        throw new IllegalArgumentException("Invalid JSON");
    }

    // Generate gift recommendations
    private static String generateRecommendations(
        String name,
        String age,
        String relationship,
        String occasion,
        String interests,
        String budget
    ) {

        String[] gifts = {
            "Personalized Gift Box",
            "Customized Photo Frame",
            "Smart Watch",
            "Premium Book Collection",
            "Personalized Accessories",
            "Wireless Earbuds",
            "Handmade Memory Scrapbook"
        };

        StringBuilder recommendations = new StringBuilder();

        recommendations.append("{\"recipient\":\"")
            .append(escapeJson(name))
            .append("\",\"recommendations\":[");

        for (int i = 0; i < gifts.length; i++) {

            if (i > 0) recommendations.append(",");

            recommendations.append("\"")
                .append(escapeJson(gifts[i]))
                .append("\"");
        }

        recommendations.append("],\"message\":\"")
            .append(escapeJson(
                "Gift suggestions prepared for " + name
                + " based on the occasion: " + occasion
                + ", interests: " + interests
                + ", and budget: " + budget
            ))
            .append("\"}");

        return recommendations.toString();
    }

    // Escape special JSON characters
    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Send response to Streamlit
    private static void sendResponse(
        HttpExchange exchange,
        int status,
        String response
    ) throws IOException {

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
