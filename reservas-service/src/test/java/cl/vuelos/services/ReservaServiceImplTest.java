package cl.vuelos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.vuelos.dto.ReservaDTO;
import cl.vuelos.dto.VueloDTO;
import cl.vuelos.entities.Reserva;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.ReservaRepository;
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
    private VuelosRemotoClient vuelosClient;

    private ReservaServiceImpl reservaService;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaServiceImpl(reservaRepo, vuelosClient, "http://localhost:8081");
    }

    @Test
    void aceptarVueloSinCuposLanza409() {
        VueloDTO vuelo = new VueloDTO(1, "Tobalaba", "2026-11-25", "Mie 25 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Uno", 0);
        when(vuelosClient.obtenerVuelo(1)).thenReturn(vuelo);
        when(reservaRepo.findById(5L)).thenReturn(Optional.empty());
        doThrow(new ApiException(409, "Ese vuelo ya no tiene cupos disponibles.")).when(vuelosClient).tomarCupo(1);

        ApiException ex = assertThrows(ApiException.class, () -> reservaService.aceptar(5L, 1));
        assertEquals(409, ex.status());
    }

    @Test
    void firmarConsentimientoActualizaReserva() {
        VueloDTO vuelo = new VueloDTO(1, "Tobalaba", "2026-11-25", "Mie 25 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Uno", 2);
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(5L);
        reserva.setVueloId(1);
        reserva.setConsentimientoFirmado(false);

        when(reservaRepo.findById(5L)).thenReturn(Optional.of(reserva));
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vuelosClient.obtenerVuelo(1)).thenReturn(vuelo);

        ReservaDTO dto = reservaService.firmar(5L, true);
        assertTrue(dto.consentimientoFirmado());
    }

    @Test
    void cancelarReservaLiberaCupo() {
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(5L);
        reserva.setVueloId(2);

        when(reservaRepo.findById(5L)).thenReturn(Optional.of(reserva));

        reservaService.cancelar(5L);

        verify(reservaRepo).delete(reserva);
        verify(vuelosClient).liberarCupo(2);
    }
}
