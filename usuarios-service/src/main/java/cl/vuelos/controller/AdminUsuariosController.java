package cl.vuelos.controller;

import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.security.AuthInterceptor;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.services.UsuarioAdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminUsuariosController {

    private final UsuarioAdminService usuarioAdminService;

    public AdminUsuariosController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @GetMapping("/socios")
    public List<Map<String, Object>> socios(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user) {
        user.exigirAdmin();
        return usuarioAdminService.listarSocios();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioPublicoDTO crearUsuario(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                          @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return usuarioAdminService.crearUsuario(body);
    }

    @GetMapping("/usuarios")
    public List<UsuarioPublicoDTO> listarUsuarios(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                                  @RequestParam(value = "rol", required = false) String rol) {
        user.exigirAdmin();
        return usuarioAdminService.listarUsuarios(rol);
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioPublicoDTO editarUsuario(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                           @PathVariable("id") long id,
                                           @RequestBody Map<String, Object> body) {
        user.exigirAdmin();
        return usuarioAdminService.editarUsuario(user, id, body);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@RequestAttribute(AuthInterceptor.USUARIO) UsuarioAutenticado user,
                                                @PathVariable("id") long id) {
        user.exigirAdmin();
        usuarioAdminService.eliminarUsuario(user, id);
        return ResponseEntity.noContent().build();
    }
}
