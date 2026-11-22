package cl.vuelos.repository;

import cl.vuelos.entities.Recuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacionRepository extends JpaRepository<Recuperacion, Long> {
}
