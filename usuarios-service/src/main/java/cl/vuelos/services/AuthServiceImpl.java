package cl.vuelos.services;

import cl.vuelos.dto.SesionDTO;
import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.entities.Recuperacion;
import cl.vuelos.entities.Usuario;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.RecuperacionRepository;
import cl.vuelos.repository.UsuarioRepository;
import cl.vuelos.security.Contrasenas;
import cl.vuelos.security.LimiteIntentos;
import cl.vuelos.security.Roles;
import cl.vuelos.security.TokenService;
import cl.vuelos.security.UsuarioAutenticado;
import cl.vuelos.util.Validaciones;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String CORREO_REPETIDO = "Ya existe una cuenta con ese correo.";
    private static final String CODIGO_INVALIDO = "El código es incorrecto o venció. Pide uno nuevo.";
    private static final int MAX_INTENTOS_RECUPERACION = 5;
    private static final int MINUTOS_VIGENCIA = 15;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepo;
    private final RecuperacionRepository recuperacionRepo;
    private final TokenService tokens;
    private final LimiteIntentos limite;
    private final boolean mostrarCodigo;

    public AuthServiceImpl(UsuarioRepository usuarioRepo,
                           RecuperacionRepository recuperacionRepo,
                           TokenService tokens,
                           LimiteIntentos limite,
                           @Value("${app.recuperacion.mostrar-codigo:true}") boolean mostrarCodigo) {
        this.usuarioRepo = usuarioRepo;
        this.recuperacionRepo = recuperacionRepo;
        this.tokens = tokens;
        this.limite = limite;
        this.mostrarCodigo = mostrarCodigo;
    }

    @Override
    @Transactional
    public SesionDTO register(Map<String, Object> body) {
        String nombre = Validaciones.texto(body.get("nombre")).trim();
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String password = Validaciones.texto(body.get("password"));

        Map<String, String> campos = new LinkedHashMap<>();
        String errorNombre = Validaciones.nombre(nombre);
        String errorEmail = Validaciones.email(email);
        String errorPassword = Validaciones.password(password);
        if (errorNombre != null) campos.put("nombre", errorNombre);
        if (errorEmail != null) campos.put("email", errorEmail);
        if (errorPassword != null) campos.put("password", errorPassword);
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        if (usuarioRepo.existsByEmail(email)) throw correoRepetido();

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPasswordHash(Contrasenas.hash(password));
        u.setRol(Roles.SOCIO);
        try {
            u = usuarioRepo.saveAndFlush(u);
        } catch (DataAccessException e) {
            if (usuarioRepo.existsByEmail(email)) throw correoRepetido();
            throw e;
        }
        return sesion(u);
    }

    @Override
    public SesionDTO login(Map<String, Object> body) {
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String password = Validaciones.texto(body.get("password"));
        if (email.isEmpty() || password.isEmpty()) {
            throw new ApiException(400, "Ingresa tu correo y tu contraseña.");
        }

        if (limite.bloqueado(email)) {
            throw new ApiException(429, "Demasiados intentos fallidos. Espera unos minutos e inténtalo otra vez.");
        }
        Usuario u = usuarioRepo.findByEmail(email).orElse(null);
        if (u == null) {
            Contrasenas.verificarFalso(password);
            limite.fallo(email);
            throw new ApiException(401, "Correo o contraseña incorrectos.");
        }
        if (!Contrasenas.verificar(password, u.getPasswordHash())) {
            limite.fallo(email);
            throw new ApiException(401, "Correo o contraseña incorrectos.");
        }
        limite.exito(email);
        return sesion(u);
    }

    @Override
    public UsuarioPublicoDTO me(UsuarioAutenticado user) {
        Usuario u = usuarioRepo.findById(user.id())
                .orElseThrow(() -> new ApiException(401, "Tu sesión no es válida."));
        return UsuarioPublicoDTO.desde(u);
    }

    @Override
    @Transactional
    public SesionDTO modificarPerfil(UsuarioAutenticado user, Map<String, Object> body) {
        Usuario u = usuarioRepo.findById(user.id())
                .orElseThrow(() -> new ApiException(401, "Tu sesión no es válida."));
        String emailActual = u.getEmail();
        String hashActual = u.getPasswordHash();

        String nombre = Validaciones.texto(body.get("nombre")).trim();
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String passwordActual = Validaciones.texto(body.get("passwordActual"));
        String passwordNueva = Validaciones.texto(body.get("passwordNueva"));
        boolean cambiaClave = !passwordNueva.isEmpty();
        boolean cambiaEmail = !email.equals(emailActual);

        Map<String, String> campos = new LinkedHashMap<>();
        String errorNombre = Validaciones.nombre(nombre);
        String errorEmail = Validaciones.email(email);
        String errorClave = cambiaClave ? Validaciones.password(passwordNueva) : null;
        if (errorNombre != null) campos.put("nombre", errorNombre);
        if (errorEmail != null) campos.put("email", errorEmail);
        if (errorClave != null) campos.put("passwordNueva", errorClave);
        if ((cambiaEmail || cambiaClave) && passwordActual.isEmpty()) {
            campos.put("passwordActual", "Ingresa tu contraseña actual para cambiar el correo o la contraseña.");
        }
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        if (cambiaEmail || cambiaClave) {
            if (limite.bloqueado(emailActual)) {
                throw new ApiException(429, "Demasiados intentos fallidos. Espera unos minutos e inténtalo otra vez.");
            }
            if (!Contrasenas.verificar(passwordActual, hashActual)) {
                limite.fallo(emailActual);
                throw new ApiException(401, "La contraseña actual no es correcta.",
                        Map.of("passwordActual", "La contraseña actual no es correcta."));
            }
            limite.exito(emailActual);
        }
        if (cambiaEmail && usuarioRepo.existsByEmailAndIdNot(email, user.id())) {
            throw correoRepetido();
        }

        u.setNombre(nombre);
        u.setEmail(email);
        if (cambiaClave) {
            u.setPasswordHash(Contrasenas.hash(passwordNueva));
        }
        try {
            u = usuarioRepo.saveAndFlush(u);
        } catch (DataAccessException e) {
            if (cambiaEmail && usuarioRepo.existsByEmailAndIdNot(email, user.id())) {
                throw correoRepetido();
            }
            throw e;
        }
        return sesion(u);
    }

    @Override
    @Transactional
    public Map<String, Object> recuperar(Map<String, Object> body) {
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String errorEmail = Validaciones.email(email);
        if (errorEmail != null) throw new ApiException(400, "Revisa los campos marcados.", Map.of("email", errorEmail));

        String clave = "recuperar:" + email;
        if (limite.bloqueado(clave)) {
            throw new ApiException(429, "Demasiados intentos. Espera unos minutos e inténtalo otra vez.");
        }
        limite.fallo(clave);

        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        Usuario u = usuarioRepo.findByEmail(email).orElse(null);
        if (u != null) {
            Recuperacion rec = recuperacionRepo.findById(u.getId()).orElseGet(() -> {
                Recuperacion nueva = new Recuperacion();
                nueva.setUsuario(u);
                return nueva;
            });
            rec.setCodigoHash(Contrasenas.hash(codigo));
            rec.setIntentos(0);
            rec.setCreadoEn(OffsetDateTime.now());
            rec.setExpiraEn(OffsetDateTime.now().plusMinutes(MINUTOS_VIGENCIA));
            recuperacionRepo.save(rec);
            log.info("Código de recuperación generado para la cuenta {} (entrega simulada: {}).", u.getId(), codigo);
        } else {
            Contrasenas.hash(codigo);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mensaje", "Si el correo está registrado, se generó un código de recuperación.");
        m.put("venceEnMinutos", MINUTOS_VIGENCIA);
        if (mostrarCodigo) m.put("codigo", codigo);
        return m;
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> restablecer(Map<String, Object> body) {
        String email = Validaciones.texto(body.get("email")).trim().toLowerCase(Locale.ROOT);
        String codigo = Validaciones.texto(body.get("codigo")).trim();
        String passwordNueva = Validaciones.texto(body.get("passwordNueva"));

        Map<String, String> campos = new LinkedHashMap<>();
        String errorEmail = Validaciones.email(email);
        String errorClave = Validaciones.password(passwordNueva);
        if (errorEmail != null) campos.put("email", errorEmail);
        if (!codigo.matches("\\d{6}")) campos.put("codigo", "El código tiene 6 dígitos.");
        if (errorClave != null) campos.put("passwordNueva", errorClave);
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        Usuario u = usuarioRepo.findByEmail(email).orElse(null);
        Recuperacion rec = u == null ? null : recuperacionRepo.findById(u.getId()).orElse(null);
        if (u == null || rec == null) {
            Contrasenas.verificarFalso(codigo);
            throw new ApiException(400, CODIGO_INVALIDO, Map.of("codigo", CODIGO_INVALIDO));
        }

        boolean vigente = rec.getExpiraEn().isAfter(OffsetDateTime.now());
        if (!vigente || rec.getIntentos() >= MAX_INTENTOS_RECUPERACION) {
            recuperacionRepo.delete(rec);
            throw new ApiException(400, CODIGO_INVALIDO, Map.of("codigo", CODIGO_INVALIDO));
        }
        if (!Contrasenas.verificar(codigo, rec.getCodigoHash())) {
            if (rec.getIntentos() + 1 >= MAX_INTENTOS_RECUPERACION) {
                recuperacionRepo.delete(rec);
            } else {
                rec.setIntentos(rec.getIntentos() + 1);
                recuperacionRepo.save(rec);
            }
            throw new ApiException(400, CODIGO_INVALIDO, Map.of("codigo", CODIGO_INVALIDO));
        }

        u.setPasswordHash(Contrasenas.hash(passwordNueva));
        usuarioRepo.save(u);
        recuperacionRepo.delete(rec);
        limite.exito(email);
        return Map.of("mensaje", "Contraseña actualizada. Ya puedes iniciar sesión.");
    }

    private SesionDTO sesion(Usuario u) {
        return new SesionDTO(tokens.emitir(u.getId(), u.getEmail(), u.getRol()), UsuarioPublicoDTO.desde(u));
    }

    private static ApiException correoRepetido() {
        return new ApiException(409, CORREO_REPETIDO, Map.of("email", "Este correo ya está registrado."));
    }
}
