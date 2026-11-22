package cl.vuelos.services;

import cl.vuelos.entities.OperacionVuelo;
import cl.vuelos.entities.Pasajero;
import java.util.List;
import java.util.Map;

public interface OperacionService {
    record RegistroPasajero(Pasajero pasajero, boolean creado) {}

    List<Pasajero> pasajeros(int vueloId);
    RegistroPasajero registrar(int vueloId, Map<String, Object> body);
    Pasajero actualizar(long id, Map<String, Object> body);
    void eliminar(long id);
    OperacionVuelo operacion(int vueloId);
    OperacionVuelo guardarOperacion(int vueloId, Map<String, Object> body);
    Map<String, Object> sincronizar(Map<String, Object> body);
}
