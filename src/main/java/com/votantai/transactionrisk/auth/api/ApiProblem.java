package com.votantai.transactionrisk.auth.api;

import java.util.List;

public record ApiProblem(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String correlationId,
        List<Object> fieldErrors) {
}
