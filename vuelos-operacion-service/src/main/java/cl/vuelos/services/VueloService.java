package cl.vuelos.services;

import cl.vuelos.entities.Vuelo;
import java.util.List;
import java.util.Map;

public interface VueloService {
    List<Vuelo> listar(String fecha, String disponibles);
    Vuelo obtenerPorId(int id);
    Vuelo crear(Map<String, Object> body);
    Vuelo editar(int id, Map<String, Object> body);
    void eliminar(int id);
}
