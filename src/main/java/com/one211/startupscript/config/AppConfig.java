package com.one211.startupscript.config;

import io.helidon.config.Config;

public class AppConfig {
    private final Config config;

    public AppConfig(Config config) {
        this.config = config;
    }

    // Flight SQL configs
    public String getUser() {
        return config.get("flight.sql.user").asString().orElse("admin");
    }

    public String getPassword() {
        return config.get("flight.sql.password").asString().orElse("admin");
    }

    public String getImage() {
        return config.get("flight.sql.image").asString().orElse("flight-sql-duckdb:latest");
    }

    // Server configs
    public int getServerPort() {
        return config.get("server.port").asInt().orElse(8080);
    }

    // Auth configs
    public String getAuthUser() {
        return config.get("auth.user").asString().orElse("admin");
    }

    public String getAuthPassword() {
        return config.get("auth.password").asString().orElse("admin");
    }

    // TLS configs
    public boolean isTlsEnabled() {
        return config.get("server.tls.enabled").asBoolean().orElse(false);
    }

    public String getKeystorePath() {
        return config.get("server.tls.private-key.keystore.resource.path").asString().orElse("keystore.p12");
    }

    public String getKeystorePassphrase() {
        return config.get("server.tls.private-key.keystore.passphrase").asString().orElse("Aman@1999");
    }

    public String getKeystoreAlias() {
        return config.get("server.tls.private-key.keystore.key-alias").asString().orElse("helidon-server");
    }

}
