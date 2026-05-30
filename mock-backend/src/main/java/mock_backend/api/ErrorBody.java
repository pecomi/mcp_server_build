package mock_backend.api;

public record ErrorBody(Error error) {

    public static ErrorBody of(String code, String message) {
        return new ErrorBody(new Error(code, message));
    }

    public record Error(String code, String message) {}
}
