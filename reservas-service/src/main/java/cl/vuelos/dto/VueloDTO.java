package cl.vuelos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "base", "fechaIso", "fechaTexto", "horario", "aeronave", "piloto", "cupos"})
public record VueloDTO(
        Integer id,
        String base,
        String fechaIso,
        String fechaTexto,
        String horario,
        String aeronave,
        String piloto,
        Integer cupos
) {}
