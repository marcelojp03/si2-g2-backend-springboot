package com.uagrm.si2g2.institucion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucion;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucionRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionRequest;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionResponse;
import com.uagrm.si2g2.institucion.dto.ConfiguracionParametroResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionInstitucionRepository configuracionRepository;
    private final InstitucionRepository institucionRepository;
    private final AuditoriaService auditoriaService;

    private final Map<UUID, Map<String, String>> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<ConfiguracionParametroResponse> listarSoportadas(UUID idInstitucion) {
        Institucion institucion = loadInstitucion(idInstitucion);
        Map<String, ConfiguracionInstitucion> configuraciones = configuracionRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(Collectors.toMap(ConfiguracionInstitucion::getClave, Function.identity()));

        return ConfiguracionCatalog.DEFINITIONS.stream()
                .map(def -> toParametroResponse(def, configuraciones.get(def.getClave()), institucion.getTipoInstitucion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getResolvedConfigurationMap(UUID idInstitucion) {
        return cache.computeIfAbsent(idInstitucion, this::loadResolvedConfigurationMap);
    }

    @Transactional(readOnly = true)
    public String getText(UUID idInstitucion, String key) {
        return getRequiredValue(idInstitucion, key);
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(UUID idInstitucion, String key) {
        return Boolean.parseBoolean(getRequiredValue(idInstitucion, key));
    }

    @Transactional(readOnly = true)
    public int getInt(UUID idInstitucion, String key) {
        return Integer.parseInt(getRequiredValue(idInstitucion, key));
    }

    @Transactional
    public ConfiguracionInstitucionResponse guardar(UUID idInstitucion, ConfiguracionInstitucionRequest request) {
        Institucion institucion = loadInstitucion(idInstitucion);
        ConfiguracionCatalog.Definition definition = ConfiguracionCatalog.find(request.getClave())
                .orElseThrow(() -> new IllegalArgumentException("La clave de configuración no está soportada: " + request.getClave()));

        String normalizedValue = validateAndNormalize(definition, request.getValor());
        ConfiguracionInstitucion existing = configuracionRepository.findByIdInstitucionAndClave(idInstitucion, request.getClave())
                .orElse(null);
        String oldValue = existing != null ? existing.getValor() : null;

        ConfiguracionInstitucion entity = existing != null ? existing : ConfiguracionInstitucion.builder()
                .idInstitucion(idInstitucion)
                .clave(definition.getClave())
                .build();

        entity.setValor(normalizedValue);
        entity.setTipoValor(definition.getTipoValor());
        entity.setDescripcion(definition.getDescripcion());

        ConfiguracionInstitucion saved = configuracionRepository.save(entity);
        cache.remove(idInstitucion);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), "CONFIGURACION",
                "GUARDAR", "configuracion_institucion", saved.getId().toString(),
                oldValue == null ? null : Map.of("valor", oldValue), Map.of("valor", normalizedValue),
                true, "Configuración guardada: " + definition.getClave());
        return ConfiguracionInstitucionResponse.from(saved);
    }

    @Transactional
    public void resetToDefault(UUID idInstitucion, String key) {
        loadInstitucion(idInstitucion);
        ConfiguracionCatalog.find(key)
                .orElseThrow(() -> new IllegalArgumentException("La clave de configuración no está soportada: " + key));
        ConfiguracionInstitucion existing = configuracionRepository.findByIdInstitucionAndClave(idInstitucion, key)
                .orElseThrow(() -> new EntityNotFoundException("No existe una configuración personalizada para la clave: " + key));
        String oldValue = existing.getValor();
        configuracionRepository.delete(existing);
        cache.remove(idInstitucion);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), "CONFIGURACION",
                "RESET_DEFAULT", "configuracion_institucion", existing.getId().toString(),
                Map.of("valor", oldValue), null, true,
                "Configuración restablecida al valor por defecto: " + key);
    }

    private ConfiguracionParametroResponse toParametroResponse(ConfiguracionCatalog.Definition definition,
                                                               ConfiguracionInstitucion override,
                                                               String institutionType) {
        String defaultValue = ConfiguracionCatalog.resolveDefaultValue(definition, institutionType);
        boolean usingDefault = override == null;
        return ConfiguracionParametroResponse.builder()
                .clave(definition.getClave())
                .nombre(definition.getNombre())
                .modulo(definition.getModulo())
                .descripcion(definition.getDescripcion())
                .obligatorio(definition.isObligatorio())
                .tipoValor(definition.getTipoValor())
                .valor(usingDefault ? defaultValue : override.getValor())
                .valorPorDefecto(defaultValue)
                .usaValorPorDefecto(usingDefault)
                .minimo(definition.getMinValue())
                .maximo(definition.getMaxValue())
                .valoresPermitidos(definition.getAllowedValues())
                .build();
    }

    private Map<String, String> loadResolvedConfigurationMap(UUID idInstitucion) {
        Institucion institucion = loadInstitucion(idInstitucion);
        Map<String, String> overrides = configuracionRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(Collectors.toMap(ConfiguracionInstitucion::getClave, ConfiguracionInstitucion::getValor));

        return ConfiguracionCatalog.DEFINITIONS.stream()
                .collect(Collectors.toMap(
                        ConfiguracionCatalog.Definition::getClave,
                        def -> overrides.getOrDefault(def.getClave(), ConfiguracionCatalog.resolveDefaultValue(def, institucion.getTipoInstitucion()))
                ));
    }

    private Institucion loadInstitucion(UUID idInstitucion) {
        return institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + idInstitucion));
    }

    private String getRequiredValue(UUID idInstitucion, String key) {
        String value = getResolvedConfigurationMap(idInstitucion).get(key);
        if (value == null) {
            throw new IllegalStateException("No existe configuración resuelta para la clave: " + key);
        }
        return value;
    }

    private String validateAndNormalize(ConfiguracionCatalog.Definition definition, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El valor es obligatorio para la clave: " + definition.getClave());
        }

        return switch (definition.getTipoValor()) {
            case "BOOLEANO" -> normalizeBoolean(definition, value);
            case "NUMERO" -> normalizeNumber(definition, value);
            default -> normalizeText(definition, value);
        };
    }

    private String normalizeBoolean(ConfiguracionCatalog.Definition definition, String value) {
        String normalized = value.trim().toLowerCase();
        if (!List.of("true", "false").contains(normalized)) {
            throw new IllegalArgumentException("El valor debe ser booleano para la clave: " + definition.getClave());
        }
        return normalized;
    }

    private String normalizeNumber(ConfiguracionCatalog.Definition definition, String value) {
        try {
            double numericValue = Double.parseDouble(value.trim());
            if (definition.getMinValue() != null && numericValue < definition.getMinValue()) {
                throw new IllegalArgumentException("El valor mínimo permitido para " + definition.getClave() + " es " + definition.getMinValue());
            }
            if (definition.getMaxValue() != null && numericValue > definition.getMaxValue()) {
                throw new IllegalArgumentException("El valor máximo permitido para " + definition.getClave() + " es " + definition.getMaxValue());
            }
            return numericValue % 1 == 0 ? String.valueOf((int) numericValue) : String.valueOf(numericValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor debe ser numérico para la clave: " + definition.getClave());
        }
    }

    private String normalizeText(ConfiguracionCatalog.Definition definition, String value) {
        String normalized = value.trim();
        if (!definition.getAllowedValues().isEmpty() && !definition.getAllowedValues().contains(normalized)) {
            throw new IllegalArgumentException("El valor permitido para " + definition.getClave() + " debe ser uno de: " + String.join(", ", definition.getAllowedValues()));
        }
        return normalized;
    }
}
