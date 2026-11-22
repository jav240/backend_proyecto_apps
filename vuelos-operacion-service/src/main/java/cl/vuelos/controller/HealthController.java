package cl.vuelos.controller;

import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.VueloRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final VueloRepository vueloRepo;

    public HealthController(VueloRepository vueloRepo) {
        this.vueloRepo = vueloRepo;
    }

    @GetMapping("/health/live")
    public Map<String, Object> live() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("servicio", "vuelos-operacion-service");
        return m;
    }

    @GetMapping("/health/ready")
    public Map<String, Object> ready() {
        return health();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            vueloRepo.count();
        } catch (DataAccessException e) {
            throw new ApiException(503, "La base de datos de vuelos no está disponible.");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("servicio", "vuelos-operacion-service");
        m.put("db", "postgres");
        return m;
    }
}
