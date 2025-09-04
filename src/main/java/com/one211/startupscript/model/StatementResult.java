package com.one211.startupscript.model;

public record StatementResult(String script, boolean success, String error) {

    public StatementResult(String script, boolean success) {
        this(script, success, null);
    }
    public StatementResult(String script, String error) {
        this(script, false, error);
    }
}
