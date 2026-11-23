package cl.vuelos.controller;

import cl.vuelos.repository.ReservaRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final ReservaRepository reservaRepo;

    public HealthController(ReservaRepository reservaRepo) {
        this.reservaRepo = reservaRepo;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "ok", true,
                "service", "reservas-service",
                "reservas", reservaRepo.count()
        );
    }
}