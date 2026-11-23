package cl.vuelos.dto;

import cl.vuelos.entities.Reserva;

public record ReservaDTO(VueloDTO vuelo, boolean consentimientoFirmado, String creadoEn) {
    public static ReservaDTO desde(Reserva r, VueloDTO vuelo) {
        return new ReservaDTO(
                vuelo,
                r.isConsentimientoFirmado(),
                r.getCreadoEn() != null ? r.getCreadoEn().toString() : null
        );
    }
}
