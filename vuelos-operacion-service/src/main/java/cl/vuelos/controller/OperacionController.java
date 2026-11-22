package cl.vuelos.controller;

import cl.vuelos.entities.OperacionVuelo;
import cl.vuelos.entities.Pasajero;
import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.OperacionService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operacion")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @GetMapping("/vuelos/{id}/pasajeros")
    public List<Pasajero> pasajeros(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                    @PathVariable("id") int id) {
        user.exigirAdmin();
        return operacionService.pasajeros(id);
    }

    @PostMapping("/vuelos/{id}/pasajeros")
    public ResponseEntity<Pasajero> registrar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                              @PathVariable("id") int id,
                                              @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        OperacionService.RegistroPasajero registro = operacionService.registrar(id, body);
        return ResponseEntity.status(registro.creado() ? HttpStatus.CREATED : HttpStatus.OK).body(registro.pasajero());
    }

    @PutMapping("/pasajeros/{id}")
    public Pasajero actualizar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                               @PathVariable("id") long id,
                               @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return operacionService.actualizar(id, body);
    }

    @DeleteMapping("/pasajeros/{id}")
    public ResponseEntity<Void> eliminar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                         @PathVariable("id") long id) {
        user.exigirAdmin();
        operacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vuelos/{id}/horometros")
    public OperacionVuelo horometros(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                     @PathVariable("id") int id) {
        user.exigirAdmin();
        return operacionService.operacion(id);
    }

    @PutMapping("/vuelos/{id}/horometros")
    public OperacionVuelo guardarHorometros(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                            @PathVariable("id") int id,
                                            @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return operacionService.guardarOperacion(id, body);
    }

    @PostMapping("/sync")
    public Map<String, Object> sincronizar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                           @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return operacionService.sincronizar(body);
    }
}
