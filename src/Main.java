import com.fastcgi.FCGIInterface;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        FCGIInterface server = new FCGIInterface();

        while (true) {
            if (server.FCGIaccept() < 0) {
                // После закрытия соединения библиотека может вернуть -1,
                // хотя серверный сокет ещё готов принимать запросы.
                if (FCGIInterface.srvSocket == null || FCGIInterface.srvSocket.isClosed()) {
                    break;
                }
                continue;
            }

            long started = System.nanoTime();
            String method = System.getProperty("REQUEST_METHOD", "");
            String query = System.getProperty("QUERY_STRING", "");
            Reply reply = process(method, query, started);

            System.out.print("Status: " + reply.status + "\r\n");
            System.out.print("Content-Type: application/json; charset=UTF-8\r\n");
            System.out.print("Cache-Control: no-store\r\n\r\n");
            System.out.print(reply.body);
            System.out.flush();
        }
    }

    static Reply process(String method, String query, long started) {
        if (!"GET".equals(method)) {
            return new Reply("405 Method Not Allowed", "{\"error\":\"Используйте GET-запрос.\"}");
        }

        Map<String, String> params;
        try {
            params = parseQuery(query);
        } catch (IllegalArgumentException error) {
            return new Reply("400 Bad Request", "{\"error\":\"Некорректные параметры запроса.\"}");
        }

        if (params.size() != 3 || !params.containsKey("x") ||
                !params.containsKey("y") || !params.containsKey("r")) {
            return new Reply("400 Bad Request", "{\"error\":\"Передайте X, Y и R по одному разу.\"}");
        }

        String xText = params.get("x");
        String yText = params.get("y");
        String rText = params.get("r");

        if (!xText.matches("-?[0-4]") || "-0".equals(xText)) {
            return new Reply("400 Bad Request", "{\"error\":\"X должен быть целым числом от -4 до 4.\"}");
        }

        if (!yText.matches("[-+]?(?:\\d+(?:[.,]\\d+)?|[.,]\\d+)")) {
            return new Reply("400 Bad Request", "{\"error\":\"Y должен быть числом от -5 до 5.\"}");
        }

        if (!("1".equals(rText) || "1.5".equals(rText) || "2".equals(rText) ||
                "2.5".equals(rText) || "3".equals(rText))) {
            return new Reply("400 Bad Request", "{\"error\":\"Выберите R из списка.\"}");
        }

        int x = Integer.parseInt(xText);
        double y;
        try {
            y = Double.parseDouble(yText.replace(',', '.'));
        } catch (NumberFormatException error) {
            return new Reply("400 Bad Request", "{\"error\":\"Y должен быть числом от -5 до 5.\"}");
        }
        if (!Double.isFinite(y) || y <= -5 || y >= 5) {
            return new Reply("400 Bad Request", "{\"error\":\"Y должен быть между -5 и 5, не включая границы.\"}");
        }

        double r = Double.parseDouble(rText);
        boolean hit = isHit(x, y, r);
        String checkedAt = Instant.now().toString();
        double durationMs = (System.nanoTime() - started) / 1_000_000.0;

        String body = "{\"x\":" + x +
                ",\"y\":" + y +
                ",\"r\":" + r +
                ",\"hit\":" + hit +
                ",\"checkedAt\":\"" + checkedAt + "\"" +
                ",\"durationMs\":" + String.format(Locale.US, "%.3f", durationMs) + "}";
        return new Reply("200 OK", body);
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query.isEmpty()) {
            return params;
        }

        for (String part : query.split("&", -1)) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Missing equals sign");
            }
            String name;
            String value;
            try {
                name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
                value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException error) {
                throw new IllegalStateException(error);
            }
            if (params.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Repeated parameter");
            }
        }
        return params;
    }

    static boolean isHit(double x, double y, double r) {
        boolean inRectangle = x >= -r && x <= 0 && y >= -r / 2 && y <= 0;
        boolean inTriangle = x >= -r && x <= 0 && y >= 0 && y <= (x + r) / 2;
        boolean inQuarterCircle = x >= 0 && y >= 0 && x * x + y * y <= r * r / 4;
        return inRectangle || inTriangle || inQuarterCircle;
    }

    static class Reply {
        final String status;
        final String body;

        Reply(String status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}
