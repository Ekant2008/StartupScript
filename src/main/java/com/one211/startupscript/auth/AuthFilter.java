package com.one211.startupscript.auth;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AuthFilter implements Filter {
    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
        String authHeader = req.headers().value(HeaderNames.create("Authorization")).orElse(null);
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            res.status(401).header("WWW-Authenticate", "Basic realm=\"startupscript\"").send("Unauthorized");
            return;
        }
        String credentials = new String(Base64.getDecoder().decode(authHeader.substring("Basic ".length())), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);
        if (parts.length != 2 || !authService.authenticate(parts[0], parts[1])) {
            res.status(401).send("Unauthorized");
            return;
        }
        req.context().register(String.class, parts[0]);
        chain.proceed();
    }
}
