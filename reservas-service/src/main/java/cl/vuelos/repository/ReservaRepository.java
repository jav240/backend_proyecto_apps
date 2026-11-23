package cl.vuelos.repository;

import cl.vuelos.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    boolean existsByVueloId(Integer vueloId);
}