package cl.vuelos.repository;

import cl.vuelos.entities.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<Usuario> findByRolOrderByNombreAsc(String rol);
    List<Usuario> findAllByOrderByNombreAscIdAsc();
}
