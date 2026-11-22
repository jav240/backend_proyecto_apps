package cl.vuelos.dto;

import cl.vuelos.entities.Reserva;
import cl.vuelos.entities.Vuelo;

public record ReservaDTO(Vuelo vuelo, boolean consentimientoFirmado, String creadoEn) {
    public static ReservaDTO desde(Reserva r) {
        return new ReservaDTO(
                r.getVuelo(),
                r.isConsentimientoFirmado(),
                r.getCreadoEn() != null ? r.getCreadoEn().toString() : null
        );
    }
}
