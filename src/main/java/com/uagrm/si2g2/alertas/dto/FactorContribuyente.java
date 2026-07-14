package com.uagrm.si2g2.alertas.dto;

/** Explica de forma visible por que el algoritmo asigno un nivel de riesgo. */
public record FactorContribuyente(
        String nombre,
        double peso,
        double valor,
        double impacto,
        String descripcion
) {}
