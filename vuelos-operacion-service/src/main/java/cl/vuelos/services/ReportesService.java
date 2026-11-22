package cl.vuelos.services;

import java.util.Map;

public interface ReportesService {
    Map<String, Object> estadisticas(String desde, String hasta);
    String generarCsv(String desde, String hasta);
}
