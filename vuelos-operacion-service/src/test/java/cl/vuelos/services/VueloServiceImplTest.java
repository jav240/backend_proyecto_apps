package cl.vuelos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.OperacionVueloRepository;
import cl.vuelos.repository.PasajeroRepository;
import cl.vuelos.repository.ReservaRepository;
import cl.vuelos.repository.VueloRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VueloServiceImplTest {

    @Mock
    private VueloRepository vueloRepo;

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private PasajeroRepository pasajeroRepo;

    @Mock
    private OperacionVueloRepository operacionRepo;

    @InjectMocks
    private VueloServiceImpl vueloService;

    @Test
    void crearVueloValidoGuardaCorrectamente() {
        when(vueloRepo.save(any(Vuelo.class))).thenAnswer(inv -> {
            Vuelo v = inv.getArgument(0);
            v.setId(99);
            return v;
        });

        Vuelo creado = vueloService.crear(Map.of(
                "base", "Tobalaba",
                "fechaIso", "2026-11-28",
                "horario", "10:00 - 11:00",
                "matricula", "CC-AAA",
                "modelo", "Cessna 172",
                "piloto", "Piloto Prueba",
                "cupos", 3
        ));

        assertEquals(99, creado.getId());
        assertEquals("Cessna 172 (CC-AAA)", creado.getAeronave());
        assertEquals(3, creado.getCupos());
    }

    @Test
    void eliminarVueloConReservasLanza409() {
        Vuelo existente = new Vuelo(4, "Tobalaba", "2026-11-28", "Sáb 28 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Prueba", 2);
        when(vueloRepo.findById(4)).thenReturn(Optional.of(existente));
        when(reservaRepo.existsByVuelo_Id(4)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> vueloService.eliminar(4));
        assertEquals(409, ex.status());
        verify(vueloRepo, never()).delete(any());
    }
}
