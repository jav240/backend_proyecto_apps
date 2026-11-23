package cl.vuelos.services;

import cl.vuelos.dto.ReservaDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReservaService {
    Optional<ReservaDTO> obtener(long usuarioId);
    ReservaDTO aceptar(long usuarioId, int vueloId);
    ReservaDTO firmar(long usuarioId, boolean firmado);
    void cancelar(long usuarioId);
    List<Map<String, Object>> listarTodasAdmin();
}
