package cl.vuelos.controller;

import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.repository.UsuarioRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/usuarios")
public class InternalUsuariosController {

    private final UsuarioRepository usuarioRepo;

    public InternalUsuariosController(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping
    public List<UsuarioPublicoDTO> todos() {
        return usuarioRepo.findAll().stream().map(UsuarioPublicoDTO::desde).toList();
    }
}
