package cl.vuelos.controller;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.ReportesService;
import cl.vuelos.services.ReservaService;
import cl.vuelos.services.VueloService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminVuelosController {

    private final VueloService vueloService;
    private final ReservaService reservaService;
    private final ReportesService reportesService;

    public AdminVuelosController(VueloService vueloService,
                                 ReservaService reservaService,
                                 ReportesService reportesService) {
        this.vueloService = vueloService;
        this.reservaService = reservaService;
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

    @GetMapping("/reservas")
    public List<Map<String, Object>> reservas(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user) {
        user.exigirAdmin();
        return reservaService.listarTodasAdmin();
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
