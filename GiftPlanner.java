
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GiftPlanner {

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
                sendResponse(exchange, 500,
                    "{\"error\":\"Unable to generate recommendations\"}");
            }
        });

        server.start();
        System.out.println("Gift Planner running on port " + port);
    }

    // Extracts string values from JSON.
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

    // Generates personalized gift recommendations.
    private static String generateRecommendations(
            String name,
            String age,
            String relationship,
            String occasion,
            String interests,
            String budget) {

        String interest = interests.toLowerCase(Locale.ROOT);
        String relation = relationship.toLowerCase(Locale.ROOT);
        String event = occasion.toLowerCase(Locale.ROOT);

        int maxBudget = extractBudget(budget);

        if (maxBudget <= 0) {
            maxBudget = 1000;
        }

        List<String> gifts = new ArrayList<>();

        // Interest-based recommendations.
        if (interest.contains("music") || interest.contains("sing")
                || interest.contains("song")) {

            addGift(gifts, "Personalized playlist",
                "A collection of songs chosen specially for " + name);
            addGift(gifts, "Mini Bluetooth speaker",
                "A compact speaker for enjoying favourite music");

        }

        if (interest.contains("book") || interest.contains("read")
                || interest.contains("novel")) {

            addGift(gifts, "Favourite genre book",
                "A book based on the recipient's reading interests");
            addGift(gifts, "Personalized bookmark",
                "A bookmark with their name or a meaningful quote");

        }

        if (interest.contains("sport") || interest.contains("cricket")
                || interest.contains("football") || interest.contains("game")) {

            addGift(gifts, "Sports accessories",
                "Useful accessories related to their favourite sport");
            addGift(gifts, "Team merchandise",
                "A T-shirt or accessory featuring their favourite team");

        }

        if (interest.contains("art") || interest.contains("draw")
                || interest.contains("paint") || interest.contains("craft")) {

            addGift(gifts, "Creative art kit",
                "A set of drawing or painting supplies");
            addGift(gifts, "Personalized sketchbook",
                "A sketchbook for saving creative ideas and artwork");

        }

        if (interest.contains("tech") || interest.contains("computer")
                || interest.contains("coding") || interest.contains("gadget")) {

            addGift(gifts, "Tech accessories",
                "Useful accessories such as a mouse pad or cable organizer");
            addGift(gifts, "Mobile stand",
                "A practical stand for watching videos or studying");

        }

        if (interest.contains("travel") || interest.contains("trip")
                || interest.contains("adventure")) {

            addGift(gifts, "Travel organizer",
                "A compact organizer for keeping travel essentials together");
            addGift(gifts, "Personalized travel journal",
                "A journal for recording memorable journeys");

        }

        if (interest.contains("photo") || interest.contains("camera")) {

            addGift(gifts, "Photo frame",
                "A frame for displaying a favourite memory");
            addGift(gifts, "Mini photo album",
                "A collection of special photographs");

        }

        if (interest.contains("fashion") || interest.contains("style")
                || interest.contains("dress")) {

            addGift(gifts, "Fashion accessory",
                "A stylish accessory suited to their taste");
            addGift(gifts, "Personalized tote bag",
                "A useful bag with a design they will love");

        }

        if (interest.contains("cook") || interest.contains("food")
                || interest.contains("bake")) {

            addGift(gifts, "Baking or cooking kit",
                "A useful starter kit for their culinary hobby");
            addGift(gifts, "Personalized recipe book",
                "A place to collect their favourite recipes");

        }

        if (interest.contains("fitness") || interest.contains("gym")
                || interest.contains("yoga")) {

            addGift(gifts, "Fitness accessories",
                "Useful workout accessories for their routine");
            addGift(gifts, "Reusable water bottle",
                "A practical bottle for workouts and everyday use");

        }

        // Age-based suggestions.
        int recipientAge = extractBudget(age);

        if (recipientAge > 0 && recipientAge <= 12) {
            addGift(gifts, "Educational activity kit",
                "A fun way to learn through creative activities");
            addGift(gifts, "Building blocks or puzzle",
                "An entertaining gift that encourages problem-solving");

        } else if (recipientAge <= 25 && recipientAge > 12) {
            addGift(gifts, "Customized phone cover",
                "A phone cover with a design matching their personality");
            addGift(gifts, "Study or desk organizer",
                "A useful accessory for keeping their workspace organized");

        } else if (recipientAge > 25) {
            addGift(gifts, "Personalized diary",
                "An elegant diary for plans, thoughts and memories");
            addGift(gifts, "Indoor decorative plant",
                "A lovely plant to brighten their living space");
        }

        // Relationship and occasion suggestions.
        if (relation.contains("friend") || relation.contains("best")) {
            addGift(gifts, "Friendship memory scrapbook",
                "A collection of memorable moments together");
        }

        if (relation.contains("mother") || relation.contains("mom")
                || relation.contains("father") || relation.contains("dad")
                || relation.contains("parent")) {
            addGift(gifts, "Personalized family photo frame",
                "A special family memory displayed beautifully");
        }

        if (relation.contains("partner") || relation.contains("husband")
                || relation.contains("wife") || relation.contains("boyfriend")
                || relation.contains("girlfriend")) {
            addGift(gifts, "Personalized memory box",
                "A box filled with meaningful notes and memories");
        }

        if (event.contains("birthday")) {
            addGift(gifts, "Birthday surprise hamper",
                "A small collection of their favourite treats and items");
        }

        if (event.contains("anniversary")) {
            addGift(gifts, "Customized couple keepsake",
                "A memorable keepsake celebrating a special occasion");
        }

        if (event.contains("graduation")) {
            addGift(gifts, "Personalized achievement frame",
                "A frame celebrating an important milestone");
        }

        // General recommendations to complete the list.
        addGift(gifts, "Customized name keychain",
            "A small personalized accessory with their name");

        addGift(gifts, "Handwritten message card",
            "A heartfelt message expressing your appreciation");

        addGift(gifts, "Chocolate and snack hamper",
            "A delightful collection of favourite treats");

        addGift(gifts, "Personalized mug",
            "A useful mug with a name, quote or special design");

        addGift(gifts, "Decorative desk accessory",
            "A thoughtful addition to their everyday workspace");

        // Select exactly seven different recommendations.
        StringBuilder json = new StringBuilder();

        json.append("{\"recipient\":\"")
            .append(escapeJson(name))
            .append("\",\"recommendations\":[");

        for (int i = 0; i < 7; i++) {

            if (i > 0) {
                json.append(",");
            }

            String gift = gifts.get(i);

            json.append("\"")
                .append(escapeJson(gift))
                .append("\"");
        }

        json.append("],\"message\":\"")
            .append(escapeJson(
                "Personalized gift suggestions created using your preferences and budget of approximately INR " + maxBudget + "."
            ))
            .append("\"}");

        return json.toString();
    }

    // Adds a recommendation only once.
    private static void addGift(
            List<String> gifts,
            String giftName,
            String description) {

        String gift = giftName + " - " + description;

        if (!gifts.contains(gift) && gifts.size() < 30) {
            gifts.add(gift);
        }
    }

    // Extracts the largest number from an age or budget field.
    private static int extractBudget(String value) {

        Matcher matcher = Pattern.compile("\\d+").matcher(value);
        int result = 0;

        while (matcher.find()) {
            try {
                result = Math.max(result,
                    Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {
                // Ignore numbers that are too large.
            }
        }

        return result;
    }

    // Escapes special characters in JSON.
    private static String escapeJson(String value) {

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Sends JSON responses to Streamlit.
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
