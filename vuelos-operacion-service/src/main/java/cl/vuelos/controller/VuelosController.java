package cl.vuelos.controller;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.services.VueloService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vuelos")
public class VuelosController {

    private final VueloService vueloService;

    public VuelosController(VueloService vueloService) {
        this.vueloService = vueloService;
    }

    @GetMapping
    public List<Vuelo> listar(@RequestParam(name = "fecha", required = false) String fecha,
                              @RequestParam(name = "disponibles", required = false) String disponibles) {
        return vueloService.listar(fecha, disponibles);
    }

    @GetMapping("/{id}")
    public Vuelo detalle(@PathVariable("id") int id) {
        return vueloService.obtenerPorId(id);
    }
}
