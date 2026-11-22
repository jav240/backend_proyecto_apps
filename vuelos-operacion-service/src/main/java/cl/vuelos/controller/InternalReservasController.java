package cl.vuelos.controller;

import cl.vuelos.repository.ReservaRepository;
import cl.vuelos.services.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/reservas")
public class InternalReservasController {

    private final ReservaRepository reservaRepo;
    private final ReservaService reservaService;

    public InternalReservasController(ReservaRepository reservaRepo, ReservaService reservaService) {
        this.reservaRepo = reservaRepo;
        this.reservaService = reservaService;
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> cancelarSiExiste(@PathVariable("usuarioId") long usuarioId) {
        if (reservaRepo.existsById(usuarioId)) {
            reservaService.cancelar(usuarioId);
        }
        return ResponseEntity.noContent().build();
    }
}
