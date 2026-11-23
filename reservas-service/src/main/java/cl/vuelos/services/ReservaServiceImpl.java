package cl.vuelos.services;

import cl.vuelos.dto.ReservaDTO;
import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.dto.VueloDTO;
import cl.vuelos.entities.Reserva;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.ReservaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaServiceImpl implements ReservaService {
    private static final Logger log = LoggerFactory.getLogger(ReservaServiceImpl.class);

    private final ReservaRepository reservaRepo;
    private final VuelosRemotoClient vuelosClient;
    private final String usuariosServiceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired 
    public ReservaServiceImpl(ReservaRepository reservaRepo,
                            VuelosRemotoClient vuelosClient,
                            @Value("${app.usuarios-service-url:http://localhost:8081}") String usuariosServiceUrl) {
        this.reservaRepo = reservaRepo;
        this.vuelosClient = vuelosClient;
        this.usuariosServiceUrl = usuariosServiceUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public Optional<ReservaDTO> obtener(long usuarioId) {
        return reservaRepo.findById(usuarioId).map(r -> {
            VueloDTO vuelo = vuelosClient.obtenerVuelo(r.getVueloId());
            return ReservaDTO.desde(r, vuelo);
        });
    }

    @Override
    @Transactional
    public ReservaDTO aceptar(long usuarioId, int vueloId) {
        VueloDTO destino = vuelosClient.obtenerVuelo(vueloId);

        Reserva actual = reservaRepo.findById(usuarioId).orElse(null);
        if (actual != null && actual.getVueloId().equals(vueloId)) {
            return ReservaDTO.desde(actual, destino);
        }

        vuelosClient.tomarCupo(vueloId);

        Integer vueloAnteriorId = actual != null ? actual.getVueloId() : null;
        if (actual == null) {
            actual = new Reserva();
            actual.setUsuarioId(usuarioId);
        }
        actual.setVueloId(vueloId);
        actual.setConsentimientoFirmado(false);
        actual.setCreadoEn(OffsetDateTime.now());
        reservaRepo.saveAndFlush(actual);

        if (vueloAnteriorId != null) {
            vuelosClient.liberarCupo(vueloAnteriorId);
        }
        VueloDTO vueloActualizado = vuelosClient.obtenerVuelo(vueloId);
        return ReservaDTO.desde(actual, vueloActualizado);
    }

    @Override
    @Transactional
    public ReservaDTO firmar(long usuarioId, boolean firmado) {
        Reserva actual = reservaRepo.findById(usuarioId)
                .orElseThrow(() -> new ApiException(404, "Aun no tienes una reserva."));
        actual.setConsentimientoFirmado(firmado);
        Reserva guardada = reservaRepo.save(actual);
        VueloDTO vuelo = vuelosClient.obtenerVuelo(guardada.getVueloId());
        return ReservaDTO.desde(guardada, vuelo);
    }

    @Override
    @Transactional
    public void cancelar(long usuarioId) {
        Reserva actual = reservaRepo.findById(usuarioId)
                .orElseThrow(() -> new ApiException(404, "No tienes una reserva para cancelar."));
        int vueloId = actual.getVueloId();
        reservaRepo.delete(actual);
        vuelosClient.liberarCupo(vueloId);
    }

    @Override
    public List<Map<String, Object>> listarTodasAdmin() {
        List<Reserva> todas = reservaRepo.findAll();
        Map<Long, UsuarioPublicoDTO> usuariosPorId = obtenerUsuariosRemotos();
        Map<Integer, VueloDTO> vuelosPorId = vuelosClient.obtenerTodosPorId();

        List<Map<String, Object>> salida = new ArrayList<>();
        for (Reserva r : todas) {
            UsuarioPublicoDTO socio = usuariosPorId.get(r.getUsuarioId());
            VueloDTO vuelo = vuelosPorId.get(r.getVueloId());
            if (vuelo == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("socio", socio != null ? socio.nombre() : "Socio #" + r.getUsuarioId());
            m.put("socioEmail", socio != null ? socio.email() : "desconocido@capebe.cl");
            m.put("vuelo", vuelo);
            m.put("consentimientoFirmado", r.isConsentimientoFirmado());
            m.put("creadoEn", r.getCreadoEn() != null ? r.getCreadoEn().toString() : null);
            salida.add(m);
        }
        salida.sort(Comparator
                .comparing((Map<String, Object> m) -> ((VueloDTO) m.get("vuelo")).fechaIso())
                .thenComparing(m -> ((VueloDTO) m.get("vuelo")).horario()));
        return salida;
    }

    private Map<Long, UsuarioPublicoDTO> obtenerUsuariosRemotos() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(usuariosServiceUrl + "/internal/usuarios"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                List<UsuarioPublicoDTO> lista = mapper.readValue(res.body(), new TypeReference<>() {});
                return lista.stream().collect(Collectors.toMap(UsuarioPublicoDTO::id, Function.identity()));
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar usuarios-service: {}", e.getMessage());
        }
        return Map.of();
    }
}