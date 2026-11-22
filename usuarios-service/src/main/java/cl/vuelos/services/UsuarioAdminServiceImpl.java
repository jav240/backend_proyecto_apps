package cl.vuelos.services;

import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.entities.Usuario;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.RecuperacionRepository;
import cl.vuelos.repository.UsuarioRepository;
import cl.vuelos.security.Contrasenas;
import cl.vuelos.security.Roles;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.util.Validaciones;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAdminServiceImpl implements UsuarioAdminService {
    private static final Logger log = LoggerFactory.getLogger(UsuarioAdminServiceImpl.class);

    private final UsuarioRepository usuarioRepo;
    private final RecuperacionRepository recuperacionRepo;
    private final String vuelosServiceUrl;
    private final HttpClient httpClient;

    public UsuarioAdminServiceImpl(UsuarioRepository usuarioRepo,
                                   RecuperacionRepository recuperacionRepo,
                                   @Value("${app.vuelos-service-url:http://localhost:8082}") String vuelosServiceUrl) {
        this.usuarioRepo = usuarioRepo;
        this.recuperacionRepo = recuperacionRepo;
        this.vuelosServiceUrl = vuelosServiceUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public List<Map<String, Object>> listarSocios() {
        return usuarioRepo.findByRolOrderByNombreAsc(Roles.SOCIO).stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("nombre", u.getNombre());
                    m.put("email", u.getEmail());
                    m.put("creadoEn", u.getCreadoEn() != null ? u.getCreadoEn().toString() : null);
                    return m;
                })
                .toList();
    }

    @Override
    @Transactional
    public UsuarioPublicoDTO crearUsuario(Map<String, Object> body) {
        String nombre = Validaciones.texto(body.get("nombre")).trim();
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String password = Validaciones.texto(body.get("password"));
        String rol = Validaciones.texto(body.get("rol")).trim().toLowerCase(Locale.ROOT);

        Map<String, String> campos = new LinkedHashMap<>();
        String errorNombre = Validaciones.nombre(nombre);
        String errorEmail = Validaciones.email(email);
        String errorPassword = Validaciones.password(password);
        if (errorNombre != null) campos.put("nombre", errorNombre);
        if (errorEmail != null) campos.put("email", errorEmail);
        if (errorPassword != null) campos.put("password", errorPassword);
        if (!Roles.valido(rol)) campos.put("rol", "El rol debe ser administrador o socio.");
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        if (usuarioRepo.existsByEmail(email)) {
            throw new ApiException(409, "Ya existe una cuenta con ese correo.", Map.of("email", "Este correo ya está registrado."));
        }

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPasswordHash(Contrasenas.hash(password));
        u.setRol(rol);
        u = usuarioRepo.save(u);
        return UsuarioPublicoDTO.desde(u);
    }

    @Override
    public List<UsuarioPublicoDTO> listarUsuarios(String rol) {
        String filtro = rol == null || rol.isBlank() ? null : rol.trim().toLowerCase(Locale.ROOT);
        if (filtro != null && !Roles.valido(filtro)) {
            throw new ApiException(400, "El rol debe ser administrador o socio.");
        }
        return usuarioRepo.findAllByOrderByNombreAscIdAsc().stream()
                .filter(u -> filtro == null || u.getRol().equals(filtro))
                .map(UsuarioPublicoDTO::desde)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioPublicoDTO editarUsuario(UsuarioAutenticado admin, long id, Map<String, Object> body) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "La cuenta no existe."));

        String nombre = Validaciones.texto(body.get("nombre")).trim();
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String rol = Validaciones.texto(body.get("rol")).trim().toLowerCase(Locale.ROOT);
        boolean cambiaClave = body.get("password") != null && !Validaciones.texto(body.get("password")).isEmpty();
        String password = cambiaClave ? Validaciones.texto(body.get("password")) : null;

        Map<String, String> campos = new LinkedHashMap<>();
        String errorNombre = Validaciones.nombre(nombre);
        String errorEmail = Validaciones.email(email);
        String errorPassword = cambiaClave ? Validaciones.password(password) : null;
        if (errorNombre != null) campos.put("nombre", errorNombre);
        if (errorEmail != null) campos.put("email", errorEmail);
        if (errorPassword != null) campos.put("password", errorPassword);
        if (!Roles.valido(rol)) campos.put("rol", "El rol debe ser administrador o socio.");
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        if (usuarioRepo.existsByEmailAndIdNot(email, id)) {
            throw new ApiException(409, "Ya existe una cuenta con ese correo.", Map.of("email", "Este correo ya está registrado."));
        }
        if (id == admin.id() && !u.getRol().equals(rol)) {
            throw new ApiException(409, "No puedes cambiar tu propio rol.", Map.of("rol", "No puedes cambiar tu propio rol."));
        }

        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        if (cambiaClave) {
            u.setPasswordHash(Contrasenas.hash(password));
        }
        u = usuarioRepo.save(u);
        return UsuarioPublicoDTO.desde(u);
    }

    @Override
    @Transactional
    public void eliminarUsuario(UsuarioAutenticado admin, long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "La cuenta no existe."));
        if (id == admin.id()) throw new ApiException(409, "No puedes eliminar tu propia cuenta.");

        cancelarReservaEnVuelosService(id);
        recuperacionRepo.deleteById(id);
        usuarioRepo.delete(u);
    }

    private void cancelarReservaEnVuelosService(long usuarioId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(vuelosServiceUrl + "/internal/reservas/" + usuarioId))
                    .timeout(Duration.ofSeconds(3))
                    .DELETE()
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("No se pudo notificar cancelación de reserva para usuario {} a vuelos-operacion-service: {}",
                    usuarioId, e.getMessage());
        }
    }
}
