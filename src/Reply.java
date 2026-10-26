public class Reply {
    final HttpStatus status;
    final String body;

    Reply(HttpStatus status, String body) {
        this.status = status;
        this.body = body;
    }
}
