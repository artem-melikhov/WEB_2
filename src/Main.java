import com.fastcgi.FCGIInterface;

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
            Reply reply;
            try {
                reply = RequestHandler.process(method, query, started);
            } catch (RuntimeException error) {
                error.printStackTrace(System.err);
                reply = new Reply(HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\":\"Ошибка сервера\"}");
            }

            System.out.print("Status: " + reply.status.headerValue() + "\r\n");
            System.out.print("Content-Type: application/json; charset=UTF-8\r\n");
            System.out.print("Cache-Control: no-store\r\n\r\n");
            System.out.print(reply.body);
            System.out.flush();
        }
    }
}
