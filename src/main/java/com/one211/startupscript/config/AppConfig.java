package com.one211.startupscript.config;

import io.helidon.config.Config;

public class AppConfig {
    private final Config config;

    public AppConfig(Config config) {this.config = config;}
    public String getUser() {return config.get("flight.sql.user").asString().get();}
    public String getPassword() {return config.get("flight.sql.password").asString().get();}
    public String getImage() {return config.get("flight.sql.image").asString().get();}
    public String getAuthUser() {return config.get("auth.user").asString().get();}
    public String getAuthPassword() {return config.get("auth.password").asString().get();}


}
