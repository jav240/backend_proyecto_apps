package cl.vuelos.controller;

import cl.vuelos.dto.SesionDTO;
import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.AuthService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionDTO register(@RequestBody Map<String, Object> body) {
        return authService.register(body);
    }

    @PostMapping("/login")
    public SesionDTO login(@RequestBody Map<String, Object> body) {
        return authService.login(body);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("usuario", authService.me(user));
        return m;
    }

    @PutMapping("/me")
    public SesionDTO modificarPerfil(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                     @RequestBody Map<String, Object> body) {
        return authService.modificarPerfil(user, body);
    }

    @PostMapping("/recuperar")
    public Map<String, Object> recuperar(@RequestBody Map<String, Object> body) {
        return authService.recuperar(body);
    }

    @PostMapping("/restablecer")
    public Map<String, Object> restablecer(@RequestBody Map<String, Object> body) {
        return authService.restablecer(body);
    }
}
