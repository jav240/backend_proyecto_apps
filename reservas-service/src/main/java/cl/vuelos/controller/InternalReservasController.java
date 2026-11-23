package cl.vuelos.controller;

import cl.vuelos.repository.ReservaRepository;
import cl.vuelos.services.ReservaService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/reservas")
public class InternalReservasController {

    private final ReservaRepository reservaRepo;
    private final ReservaService reservaService;

    public InternalReservasController(ReservaRepository reservaRepo, ReservaService reservaService) {
        this.reservaRepo = reservaRepo;
        this.reservaService = reservaService;
    }

    @GetMapping("/existe-vuelo/{vueloId}")
    public Map<String, Boolean> existeVuelo(@PathVariable("vueloId") int vueloId) {
        return Map.of("existe", reservaRepo.existsByVueloId(vueloId));
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> cancelarSiExiste(@PathVariable("usuarioId") long usuarioId) {
        if (reservaRepo.existsById(usuarioId)) {
            reservaService.cancelar(usuarioId);
        }
        return ResponseEntity.noContent().build();
    }
}