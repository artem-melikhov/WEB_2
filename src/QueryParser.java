import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QueryParser {
    static class RepeatedParameterException extends IllegalArgumentException {
    }

    public static Map<String, String> parse(String query) {
        Map<String, String> params = new HashMap<>();
        if (query.isEmpty()) {
            return params;
        }

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);

            if (!("x".equals(name) || "y".equals(name) || "r".equals(name))) {
                continue;
            }
            if (pair.length != 2) {
                throw new IllegalArgumentException("Пропущено равно между ключом и значением");
            }

            String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            if (params.putIfAbsent(name, value) != null) {
                throw new RepeatedParameterException();
            }
        }
        return params;
    }
}
