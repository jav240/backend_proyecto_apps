package cl.vuelos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.vuelos.dto.ReservaDTO;
import cl.vuelos.entities.Reserva;
import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.ReservaRepository;
import cl.vuelos.repository.VueloRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private VueloRepository vueloRepo;

    @Mock
    private EntityManager entityManager;

    private ReservaServiceImpl reservaService;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaServiceImpl(reservaRepo, vueloRepo, entityManager, "http://localhost:8081");
    }

    @Test
    void aceptarVueloSinCuposLanza409() {
        Vuelo vuelo = new Vuelo(1, "Tobalaba", "2026-11-25", "Mié 25 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Uno", 0);
        when(vueloRepo.findById(1)).thenReturn(Optional.of(vuelo));
        when(reservaRepo.findById(5L)).thenReturn(Optional.empty());
        when(vueloRepo.tomarCupo(1)).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> reservaService.aceptar(5L, 1));
        assertEquals(409, ex.status());
    }

    @Test
    void firmarConsentimientoActualizaReserva() {
        Vuelo vuelo = new Vuelo(1, "Tobalaba", "2026-11-25", "Mié 25 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Uno", 2);
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(5L);
        reserva.setVuelo(vuelo);
        reserva.setConsentimientoFirmado(false);

        when(reservaRepo.findById(5L)).thenReturn(Optional.of(reserva));
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaDTO dto = reservaService.firmar(5L, true);
        assertTrue(dto.consentimientoFirmado());
    }

    @Test
    void cancelarReservaLiberaCupo() {
        Vuelo vuelo = new Vuelo(2, "Talcahuano", "2026-11-28", "Sáb 28 nov 2026", "11:00 - 12:00", "Piper PA-28 (CC-BBB)", "Piloto Dos", 1);
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(5L);
        reserva.setVuelo(vuelo);

        when(reservaRepo.findById(5L)).thenReturn(Optional.of(reserva));

        reservaService.cancelar(5L);

        verify(reservaRepo).delete(reserva);
        verify(vueloRepo).liberarCupo(2);
    }
}
