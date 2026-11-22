package cl.vuelos.controller;

import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final UsuarioRepository usuarioRepo;

    public HealthController(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping("/health/live")
    public Map<String, Object> live() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("servicio", "usuarios-service");
        return m;
    }

    @GetMapping("/health/ready")
    public Map<String, Object> ready() {
        return health();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            usuarioRepo.count();
        } catch (DataAccessException e) {
            throw new ApiException(503, "La base de datos de usuarios no está disponible.");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("servicio", "usuarios-service");
        m.put("db", "postgres");
        return m;
    }
}
