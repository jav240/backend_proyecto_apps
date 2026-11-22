package cl.vuelos.config;

import cl.vuelos.entities.Usuario;
import cl.vuelos.repository.UsuarioRepository;
import cl.vuelos.security.Contrasenas;
import cl.vuelos.security.Roles;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepo;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UsuarioRepository usuarioRepo,
                           @Value("${app.admin-email:}") String adminEmail,
                           @Value("${app.admin-password:}") String adminPassword) {
        this.usuarioRepo = usuarioRepo;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @PostConstruct
    @Transactional
    public void init() {
        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            log.warn("ADMIN_EMAIL no está definido: no hay administrador.");
            return;
        }
        Usuario admin = usuarioRepo.findByEmail(email).orElse(null);
        if (admin == null) {
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("ADMIN_EMAIL está definido pero ADMIN_PASSWORD no: no se crea el administrador.");
                return;
            }
            admin = new Usuario();
            admin.setNombre("Administrador CAPEBE");
            admin.setEmail(email);
            admin.setPasswordHash(Contrasenas.hash(adminPassword));
        }
        admin.setRol(Roles.ADMINISTRADOR);
        usuarioRepo.save(admin);
        log.info("Administrador listo en usuarios-service: {}", email);
    }
}
