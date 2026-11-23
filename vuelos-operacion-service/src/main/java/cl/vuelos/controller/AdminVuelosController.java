package cl.vuelos.controller;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.ReportesService;
import cl.vuelos.services.VueloService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminVuelosController {

    private final VueloService vueloService;
    private final ReportesService reportesService;

    public AdminVuelosController(VueloService vueloService, ReportesService reportesService) {
        this.vueloService = vueloService;
        this.reportesService = reportesService;
    }

    @PostMapping("/vuelos")
    @ResponseStatus(HttpStatus.CREATED)
    public Vuelo crearVuelo(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                            @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return vueloService.crear(body);
    }

    @PutMapping("/vuelos/{id}")
    public Vuelo editarVuelo(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                             @PathVariable("id") int id,
                             @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return vueloService.editar(id, body);
    }

    @DeleteMapping("/vuelos/{id}")
    public ResponseEntity<Void> eliminarVuelo(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                              @PathVariable("id") int id) {
        user.exigirAdmin();
        vueloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                            @RequestParam(name = "desde", required = false) String desde,
                                            @RequestParam(name = "hasta", required = false) String hasta) {
        user.exigirAdmin();
        return reportesService.estadisticas(desde, hasta);
    }

    @GetMapping("/reportes/vuelos.csv")
    public ResponseEntity<String> csv(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                      @RequestParam(name = "desde", required = false) String desde,
                                      @RequestParam(name = "hasta", required = false) String hasta) {
        user.exigirAdmin();
        String contenido = reportesService.generarCsv(desde, hasta);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vuelos-capebe.csv\"")
                .body(contenido);
    }
}
