package cl.vuelos.controller;

import cl.vuelos.dto.ReservaDTO;
import cl.vuelos.exception.ApiException;
import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.ReservaService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reserva")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public Map<String, Object> obtener(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user) {
        user.exigirSocio();
        return envolver(reservaService.obtener(user.id()).orElse(null));
    }

    @PutMapping
    public Map<String, Object> aceptar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                       @RequestBody Map<String, Object> body) {
        user.exigirSocio();
        Object valor = body.get("vueloId");
        if (!(valor instanceof Number)) throw new ApiException(400, "vueloId debe ser un número entero.");
        double numero = ((Number) valor).doubleValue();
        if (numero != Math.rint(numero) || Math.abs(numero) > Integer.MAX_VALUE) {
            throw new ApiException(400, "vueloId debe ser un número entero.");
        }
        return envolver(reservaService.aceptar(user.id(), (int) numero));
    }

    @PutMapping("/consentimiento")
    public Map<String, Object> consentimiento(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                              @RequestBody Map<String, Object> body) {
        user.exigirSocio();
        Object firmado = body.get("firmado");
        if (!(firmado instanceof Boolean)) throw new ApiException(400, "firmado debe ser true o false.");
        return envolver(reservaService.firmar(user.id(), (Boolean) firmado));
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user) {
        user.exigirSocio();
        reservaService.cancelar(user.id());
        return ResponseEntity.noContent().build();
    }

    private static Map<String, Object> envolver(ReservaDTO reserva) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reserva", reserva);
        return m;
    }
}
