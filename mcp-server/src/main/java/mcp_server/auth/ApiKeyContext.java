package mcp_server.auth;

public final class ApiKeyContext {

    private static final ThreadLocal<AuthenticatedClient> CONTEXT = new ThreadLocal<>();

    private ApiKeyContext() {
    }

    public static void set(AuthenticatedClient client) {
        CONTEXT.set(client);
    }

    public static AuthenticatedClient get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}