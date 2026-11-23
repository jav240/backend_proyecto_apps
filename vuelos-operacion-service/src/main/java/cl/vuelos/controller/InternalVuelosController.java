package cl.vuelos.controller;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.VueloRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/vuelos")
public class InternalVuelosController {

    private final VueloRepository vueloRepo;

    public InternalVuelosController(VueloRepository vueloRepo) {
        this.vueloRepo = vueloRepo;
    }

    @GetMapping
    public List<Vuelo> listarTodos() {
        return vueloRepo.findAll();
    }

    @GetMapping("/{id}")
    public Vuelo obtener(@PathVariable("id") int id) {
        return vueloRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "El vuelo no existe."));
    }

    @PostMapping("/{id}/tomar-cupo")
    @Transactional
    public ResponseEntity<Void> tomarCupo(@PathVariable("id") int id) {
        int afectados = vueloRepo.tomarCupo(id);
        if (afectados == 0) {
            throw new ApiException(409, "Ese vuelo ya no tiene cupos disponibles.");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/liberar-cupo")
    @Transactional
    public ResponseEntity<Void> liberarCupo(@PathVariable("id") int id) {
        vueloRepo.liberarCupo(id);
        return ResponseEntity.ok().build();
    }
}
