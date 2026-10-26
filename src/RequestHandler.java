import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

public class RequestHandler {
    public static Reply process(String method, String query, long started) {
        if (!"GET".equals(method)) {
            return new Reply(HttpStatus.METHOD_NOT_ALLOWED, "{\"error\":\"Используйте GET-запрос.\"}");
        }

        Map<String, String> params;
        try {
            params = QueryParser.parse(query);
        } catch (QueryParser.RepeatedParameterException error) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Передайте X, Y и R по одному разу\"}");
        } catch (IllegalArgumentException error) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Некорректные параметры запроса.\"}");
        }

        if (!params.containsKey("x") || !params.containsKey("y") || !params.containsKey("r")) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Передайте X, Y и R по одному разу\"}");
        }

        String xText = params.get("x");
        String yText = params.get("y");
        String rText = params.get("r");

        if (!xText.matches("-?[0-4]")) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"X должен быть целым числом от -4 до 4\"}");
        }

        if (!yText.matches("[-+]?(?:\\d+(?:[.,]\\d+)?|[.,]\\d+)")) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Y должен быть числом\"}");
        }

        if (!("1".equals(rText) || "1.5".equals(rText) || "2".equals(rText) || "2.5".equals(rText) || "3".equals(rText))) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Выберите R из списка\"}");
        }

        int x = Integer.parseInt(xText);
        BigDecimal y;
        try {
            y = new BigDecimal(yText.replace(',', '.'));
        } catch (NumberFormatException error) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Y должен быть числом от -5 до 5\"}");
        }
        if (y.compareTo(BigDecimal.valueOf(-5)) <= 0 || y.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return new Reply(HttpStatus.BAD_REQUEST, "{\"error\":\"Y должен быть между -5 и 5, не включая границы\"}");
        }

        double r = Double.parseDouble(rText);
        boolean hit = AreaChecker.isHit(BigDecimal.valueOf(x), y, new BigDecimal(rText));
        String checkedAt = Instant.now().toString();
        double durationMs = (System.nanoTime() - started) / 1_000_000.0;

        String body = "{\"x\":" + x +
                ",\"y\":" + y.toPlainString() +
                ",\"r\":" + r +
                ",\"hit\":" + hit +
                ",\"checkedAt\":\"" + checkedAt + "\"" +
                ",\"durationMs\":" + String.format(Locale.US, "%.3f", durationMs) + "}";
        return new Reply(HttpStatus.OK, body);
    }
}
