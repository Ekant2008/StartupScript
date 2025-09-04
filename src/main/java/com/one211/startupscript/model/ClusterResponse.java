package com.one211.startupscript.model;

import java.util.List;

public record ClusterResponse(
        String orgId,
        String clusterName,
        String status,
        String endpoint,
        List<StatementResult> statements
) {}
