package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocentesAsignacionesReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "DOCENTES_ASIGNACIONES";

    public DocentesAsignacionesReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override public String codigo() { return CODIGO; }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Docentes y asignaciones", "Asignaciones docentes por gestión, curso, paralelo y materia", "PREDEFINIDO", false,
                List.of(filter("idGestion", "Gestión académica", "uuid", false), filter("idDocente", "Docente", "uuid", false), filter("idMateria", "Materia", "uuid", false), filter("idCurso", "Curso", "uuid", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE ad.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idGestion", "ad.id_gestion_academica");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idDocente", "d.id");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idMateria", "m.id");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idCurso", "c.id");

        String sql = """
                SELECT d.codigo AS codigo_docente, CONCAT(d.apellidos, ' ', d.nombres) AS docente,
                       m.nombre AS materia, c.nombre AS curso, p.nombre AS paralelo,
                       ga.nombre AS gestion, ad.carga_horaria, ad.estado
                FROM asignacion_docente ad
                JOIN docente d ON d.id = ad.id_docente AND d.id_institucion = ad.id_institucion
                JOIN materia m ON m.id = ad.id_materia AND m.id_institucion = ad.id_institucion
                JOIN paralelo p ON p.id = ad.id_paralelo AND p.id_institucion = ad.id_institucion
                JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion
                JOIN gestion_academica ga ON ga.id = ad.id_gestion_academica AND ga.id_institucion = ad.id_institucion
                """ + where + " ORDER BY d.apellidos, d.nombres, c.nombre, p.nombre, m.nombre";

        return ReporteQuerySupport.execute(jdbc, metadata().nombre(), context.usuarioNombre(), ReporteQuerySupport.headerFilters(context),
                List.of(col("codigoDocente", "Código", "text"), col("docente", "Docente", "text"), col("materia", "Materia", "text"), col("curso", "Curso", "text"), col("paralelo", "Paralelo", "text"), col("gestion", "Gestión", "text"), col("cargaHoraria", "Carga horaria", "number"), col("estado", "Estado", "text")),
                sql, params, context.page(), context.size(), null);
    }
}
