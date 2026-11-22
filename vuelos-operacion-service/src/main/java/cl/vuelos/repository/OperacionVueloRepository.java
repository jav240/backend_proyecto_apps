package cl.vuelos.repository;

import cl.vuelos.entities.OperacionVuelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionVueloRepository extends JpaRepository<OperacionVuelo, Integer> {
    List<OperacionVuelo> findByVueloIdIn(List<Integer> vueloIds);
}
