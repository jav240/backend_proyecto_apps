package cl.vuelos.repository;

import cl.vuelos.entities.Pasajero;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {

    List<Pasajero> findByVuelo_IdOrderByNombreAscIdAsc(Integer vueloId);

    Optional<Pasajero> findByIdCliente(String idCliente);

    boolean existsByVuelo_IdAndCedula(Integer vueloId, String cedula);

    boolean existsByVuelo_Id(Integer vueloId);

    @Query("SELECT COUNT(p) FROM Pasajero p WHERE p.abordo = true AND p.vuelo.id = :vueloId")
    long contarAbordoPorVuelo(@Param("vueloId") Integer vueloId);

    @Query("SELECT COUNT(DISTINCT p.cedula) FROM Pasajero p WHERE p.abordo = true AND p.vuelo.id IN :vueloIds")
    long contarPersonasDistintas(@Param("vueloIds") List<Integer> vueloIds);

    @Query("SELECT COUNT(DISTINCT p.cedula) FROM Pasajero p WHERE p.abordo = true AND p.categoria = :categoria AND p.vuelo.id IN :vueloIds")
    long contarPersonasDistintasPorCategoria(@Param("vueloIds") List<Integer> vueloIds, @Param("categoria") String categoria);
}
