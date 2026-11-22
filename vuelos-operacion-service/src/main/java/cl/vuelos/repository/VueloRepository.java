package cl.vuelos.repository;

import cl.vuelos.entities.Vuelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VueloRepository extends JpaRepository<Vuelo, Integer> {

    List<Vuelo> findAllByOrderByFechaIsoAscHorarioAscIdAsc();

    @Query("SELECT v FROM Vuelo v WHERE (:fecha IS NULL OR v.fechaIso = :fecha) "
            + "AND (:soloDisponibles = false OR v.cupos > 0) "
            + "ORDER BY v.fechaIso ASC, v.horario ASC, v.id ASC")
    List<Vuelo> buscarConFiltros(@Param("fecha") String fecha, @Param("soloDisponibles") boolean soloDisponibles);

    @Query("SELECT v FROM Vuelo v WHERE (:desde IS NULL OR v.fechaIso >= :desde) "
            + "AND (:hasta IS NULL OR v.fechaIso <= :hasta) "
            + "ORDER BY v.fechaIso ASC, v.horario ASC, v.id ASC")
    List<Vuelo> buscarPorRango(@Param("desde") String desde, @Param("hasta") String hasta);

    @Modifying
    @Query("UPDATE Vuelo v SET v.cupos = v.cupos - 1 WHERE v.id = :id AND v.cupos > 0")
    int tomarCupo(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Vuelo v SET v.cupos = v.cupos + 1 WHERE v.id = :id")
    int liberarCupo(@Param("id") Integer id);
}
