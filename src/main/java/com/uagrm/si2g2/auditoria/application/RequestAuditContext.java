package com.uagrm.si2g2.auditoria.application;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestAuditContext {

    private final String ip;
    private final String userAgent;
    private final String platform;
    private final String metodoHttp;
    private final String rutaRecurso;
}
