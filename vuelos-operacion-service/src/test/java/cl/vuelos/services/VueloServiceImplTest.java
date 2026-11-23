package cl.vuelos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.OperacionVueloRepository;
import cl.vuelos.repository.PasajeroRepository;
import cl.vuelos.repository.VueloRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VueloServiceImplTest {

    @Mock
    private VueloRepository vueloRepo;

    @Mock
    private PasajeroRepository pasajeroRepo;

    @Mock
    private OperacionVueloRepository operacionRepo;

    private VueloServiceImpl vueloService;

    @BeforeEach
    void setUp() {
        vueloService = new VueloServiceImpl(vueloRepo, pasajeroRepo, operacionRepo, "http://localhost:8083");
    }

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
    void eliminarVueloConPasajerosLanza409() {
        Vuelo existente = new Vuelo(4, "Tobalaba", "2026-11-28", "Sab 28 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Prueba", 2);
        when(vueloRepo.findById(4)).thenReturn(Optional.of(existente));
        when(pasajeroRepo.existsByVuelo_Id(4)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> vueloService.eliminar(4));
        assertEquals(409, ex.status());
        verify(vueloRepo, never()).delete(any());
    }
}