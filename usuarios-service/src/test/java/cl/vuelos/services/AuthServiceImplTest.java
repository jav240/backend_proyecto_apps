package cl.vuelos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.vuelos.dto.SesionDTO;
import cl.vuelos.entities.Usuario;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.RecuperacionRepository;
import cl.vuelos.repository.UsuarioRepository;
import cl.vuelos.security.Contrasenas;
import cl.vuelos.security.LimiteIntentos;
import cl.vuelos.security.Roles;
import cl.vuelos.security.TokenService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private RecuperacionRepository recuperacionRepo;

    private TokenService tokenService;
    private LimiteIntentos limiteIntentos;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService("secreto-de-pruebas-unitarias-123456", 3600);
        limiteIntentos = new LimiteIntentos();
        authService = new AuthServiceImpl(usuarioRepo, recuperacionRepo, tokenService, limiteIntentos, true);
    }

    @Test
    void registerExitosoCreaSocioYDevuelveToken() {
        when(usuarioRepo.existsByEmail("ana@capebe.cl")).thenReturn(false);
        when(usuarioRepo.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        SesionDTO sesion = authService.register(Map.of(
                "nombre", "Ana Silva",
                "email", "ana@capebe.cl",
                "password", "Clave123"
        ));

        assertNotNull(sesion.token());
        assertEquals("ana@capebe.cl", sesion.usuario().email());
        assertEquals(Roles.SOCIO, sesion.usuario().rol());
    }

    @Test
    void registerConCorreoDuplicadoLanza409() {
        when(usuarioRepo.existsByEmail("ana@capebe.cl")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(Map.of(
                "nombre", "Ana Silva",
                "email", "ana@capebe.cl",
                "password", "Clave123"
        )));

        assertEquals(409, ex.status());
    }

    @Test
    void loginExitosoYFallido() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Admin CAPEBE");
        u.setEmail("admin@capebe.cl");
        u.setPasswordHash(Contrasenas.hash("Admin12345"));
        u.setRol(Roles.ADMINISTRADOR);

        when(usuarioRepo.findByEmail("admin@capebe.cl")).thenReturn(Optional.of(u));

        SesionDTO ok = authService.login(Map.of(
                "email", "admin@capebe.cl",
                "password", "Admin12345"
        ));
        assertEquals(Roles.ADMINISTRADOR, ok.usuario().rol());
        assertTrue(tokenService.verificar(ok.token()).isPresent());

        ApiException fallo = assertThrows(ApiException.class, () -> authService.login(Map.of(
                "email", "admin@capebe.cl",
                "password", "ClaveIncorrecta9"
        )));
        assertEquals(401, fallo.status());
    }
}
