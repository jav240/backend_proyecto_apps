package cl.vuelos.services;

import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.security.UsuarioAutenticado;
import java.util.List;
import java.util.Map;

public interface UsuarioAdminService {
    List<Map<String, Object>> listarSocios();
    UsuarioPublicoDTO crearUsuario(Map<String, Object> body);
    List<UsuarioPublicoDTO> listarUsuarios(String rol);
    UsuarioPublicoDTO editarUsuario(UsuarioAutenticado admin, long id, Map<String, Object> body);
    void eliminarUsuario(UsuarioAutenticado admin, long id);
}
