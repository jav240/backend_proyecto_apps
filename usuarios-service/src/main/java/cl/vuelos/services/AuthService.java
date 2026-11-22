package cl.vuelos.services;

import cl.vuelos.dto.SesionDTO;
import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.security.UsuarioAutenticado;
import java.util.Map;

public interface AuthService {
    SesionDTO register(Map<String, Object> body);
    SesionDTO login(Map<String, Object> body);
    UsuarioPublicoDTO me(UsuarioAutenticado user);
    SesionDTO modificarPerfil(UsuarioAutenticado user, Map<String, Object> body);
    Map<String, Object> recuperar(Map<String, Object> body);
    Map<String, Object> restablecer(Map<String, Object> body);
}
