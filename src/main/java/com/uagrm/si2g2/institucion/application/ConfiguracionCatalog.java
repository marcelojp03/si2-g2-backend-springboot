package com.uagrm.si2g2.institucion.application;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConfiguracionCatalog {

    private ConfiguracionCatalog() {}

    public static final List<Definition> DEFINITIONS = List.of(
            Definition.text("NOMBRE_CORTO", "Nombre corto", "IDENTIDAD", "Nombre abreviado visible de la institución", true, "Institución"),
            Definition.text("DESCRIPCION", "Descripción", "IDENTIDAD", "Descripción institucional breve", false, ""),
            Definition.text("TELEFONO_CONTACTO", "Teléfono de contacto", "IDENTIDAD", "Teléfono institucional principal", false, ""),
            Definition.text("CORREO_CONTACTO", "Correo de contacto", "IDENTIDAD", "Correo institucional de referencia", false, ""),
            Definition.text("SITIO_WEB", "Sitio web", "IDENTIDAD", "URL institucional", false, ""),
            Definition.text("COLOR_PRIMARIO", "Color primario", "IDENTIDAD", "Color principal del branding institucional", false, "#0a2e60"),
            Definition.bool("MATRICULA_HABILITADA", "Matrícula habilitada", "OPERACION", "Permite nuevas inscripciones", true, true),
            Definition.number("MAX_ALUMNOS_AULA", "Máximo de alumnos por aula", "OPERACION", "Capacidad máxima sugerida por paralelo", false, 30d, 1d, 100d),
            Definition.number("ESCALA_CALIFICACION", "Escala de calificación", "EVALUACION", "Nota máxima de la escala institucional", true, 100d, 1d, 1000d),
            Definition.number("NOTA_MINIMA_APROBACION", "Nota mínima de aprobación", "EVALUACION", "Puntaje mínimo para aprobar", true, 51d, 1d, 1000d),
            Definition.number("NOTA_MINIMA_RECUPERACION", "Nota mínima de recuperación", "EVALUACION", "Puntaje mínimo para recuperación", false, 36d, 1d, 1000d),
            Definition.select("TIPO_PERIODOS", "Tipo de periodos", "EVALUACION", "Modelo de división de la gestión académica", true, "BIMESTRAL",
                    List.of("ANUAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL")),
            Definition.number("CANTIDAD_PERIODOS", "Cantidad de periodos", "EVALUACION", "Cantidad de periodos internos por gestión", true, 4d, 1d, 12d),
            Definition.bool("PERMITE_RECUPERACION", "Permite recuperación", "EVALUACION", "Habilita procesos de recuperación académica", true, true),
            Definition.select("REDONDEO_NOTAS", "Redondeo de notas", "EVALUACION", "Regla de redondeo al calcular notas finales", true, "MATEMATICO",
                    List.of("NINGUNO", "MATEMATICO", "HACIA_ARRIBA", "HACIA_ABAJO")),
            Definition.bool("CONTROL_ASISTENCIA_OBLIGATORIO", "Control de asistencia obligatorio", "ASISTENCIA", "Exige registrar asistencia para operar la gestión", true, true),
            Definition.number("PORCENTAJE_ASISTENCIA_MINIMO", "Asistencia mínima", "ASISTENCIA", "Porcentaje mínimo de asistencia esperado", false, 75d, 0d, 100d),
            Definition.bool("USA_TURNOS", "Usa turnos", "ESTRUCTURA", "Indica si la institución opera con turnos", true, true),
            Definition.bool("USA_AREAS_CURRICULARES", "Usa áreas curriculares", "ESTRUCTURA", "Activa agrupación por áreas curriculares", true, true),
            Definition.select("FORMATO_BOLETIN", "Formato de boletín", "REPORTES", "Plantilla base para boletines académicos", true, "ESTANDAR",
                    List.of("ESTANDAR", "DETALLADO", "RESUMIDO")),
            Definition.text("FORMATO_CODIGO_ESTUDIANTE", "Formato de código de estudiante", "IDENTIDAD", "Prefijo o patrón visual del código estudiantil", false, "EST-{ANIO}-{SEQ}")
    );

    private static final Map<String, Map<String, String>> TYPE_DEFAULTS = Map.of(
            "FISCAL", Map.of(
                    "TIPO_PERIODOS", "BIMESTRAL",
                    "CANTIDAD_PERIODOS", "4",
                    "CONTROL_ASISTENCIA_OBLIGATORIO", "true",
                    "PORCENTAJE_ASISTENCIA_MINIMO", "80"
            ),
            "PRIVADO", Map.of(
                    "TIPO_PERIODOS", "TRIMESTRAL",
                    "CANTIDAD_PERIODOS", "3",
                    "FORMATO_BOLETIN", "DETALLADO"
            ),
            "CONVENIO", Map.of(
                    "TIPO_PERIODOS", "BIMESTRAL",
                    "CANTIDAD_PERIODOS", "4",
                    "FORMATO_BOLETIN", "ESTANDAR"
            )
    );

    public static Optional<Definition> find(String key) {
        return DEFINITIONS.stream().filter(def -> def.getClave().equals(key)).findFirst();
    }

    public static String resolveDefaultValue(Definition definition, String institutionType) {
        if (institutionType != null) {
            String override = TYPE_DEFAULTS.getOrDefault(institutionType.toUpperCase(), Map.of()).get(definition.getClave());
            if (override != null) {
                return override;
            }
        }
        return definition.getDefaultValue();
    }

    @Getter
    @RequiredArgsConstructor
    public static class Definition {
        private final String clave;
        private final String nombre;
        private final String modulo;
        private final String descripcion;
        private final boolean obligatorio;
        private final String tipoValor;
        private final String defaultValue;
        private final Double minValue;
        private final Double maxValue;
        private final List<String> allowedValues;

        public static Definition text(String clave, String nombre, String modulo, String descripcion, boolean obligatorio, String defaultValue) {
            return new Definition(clave, nombre, modulo, descripcion, obligatorio, "TEXTO", defaultValue, null, null, List.of());
        }

        public static Definition bool(String clave, String nombre, String modulo, String descripcion, boolean obligatorio, boolean defaultValue) {
            return new Definition(clave, nombre, modulo, descripcion, obligatorio, "BOOLEANO", String.valueOf(defaultValue), null, null, List.of("true", "false"));
        }

        public static Definition number(String clave, String nombre, String modulo, String descripcion, boolean obligatorio,
                                        Double defaultValue, Double minValue, Double maxValue) {
            return new Definition(clave, nombre, modulo, descripcion, obligatorio, "NUMERO", stripTrailingZero(defaultValue), minValue, maxValue, List.of());
        }

        public static Definition select(String clave, String nombre, String modulo, String descripcion, boolean obligatorio,
                                        String defaultValue, List<String> allowedValues) {
            return new Definition(clave, nombre, modulo, descripcion, obligatorio, "TEXTO", defaultValue, null, null, allowedValues);
        }

        private static String stripTrailingZero(Double value) {
            return value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
        }
    }
}
