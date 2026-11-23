package cl.vuelos.controller;

import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.ReservaService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reservas")
public class AdminReservasController {

    private final ReservaService reservaService;

    public AdminReservasController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user) {
        user.exigirAdmin();
        return reservaService.listarTodasAdmin();
    }
}