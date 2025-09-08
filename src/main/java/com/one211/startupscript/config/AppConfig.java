package com.one211.startupscript.config;

import io.helidon.common.config.Config;

public class AppConfig {
    private final Config config;

    public AppConfig() {this.config = Config.create();}
    public String getUser() {return config.get("flight.sql.user").asString().get();}
    public String getPassword() {return config.get("flight.sql.password").asString().get();}
    public String getImage() {return config.get("flight.sql.image").asString().orElse("flight-sql-duckdb:latest");}

    public int getServerPort() {
        return Integer.parseInt(config.get("server.port").asString().get());
    }

    public String getAuthUser() {
        return config.get("auth.user").asString().orElse("admin");
    }
    public String getAuthPassword() {
        return config.get("auth.password").asString().orElse("admin");
    }}