package cl.vuelos.services;

import cl.vuelos.dto.ReservaDTO;
import cl.vuelos.dto.UsuarioPublicoDTO;
import cl.vuelos.entities.Reserva;
import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.ReservaRepository;
import cl.vuelos.repository.VueloRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final VueloRepository vueloRepo;
    private final EntityManager entityManager;
    private final String usuariosServiceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ReservaServiceImpl(ReservaRepository reservaRepo,
                              VueloRepository vueloRepo,
                              EntityManager entityManager,
                              @Value("${app.usuarios-service-url:http://localhost:8081}") String usuariosServiceUrl) {
        this.reservaRepo = reservaRepo;
        this.vueloRepo = vueloRepo;
        this.entityManager = entityManager;
        this.usuariosServiceUrl = usuariosServiceUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public Optional<ReservaDTO> obtener(long usuarioId) {
        return reservaRepo.findById(usuarioId).map(ReservaDTO::desde);
    }

    @Override
    @Transactional
    public ReservaDTO aceptar(long usuarioId, int vueloId) {
        Vuelo destino = vueloRepo.findById(vueloId)
                .orElseThrow(() -> new ApiException(404, "El vuelo no existe."));

        Reserva actual = reservaRepo.findById(usuarioId).orElse(null);
        if (actual != null && actual.getVuelo().getId().equals(vueloId)) {
            return ReservaDTO.desde(actual);
        }

        int afectados = vueloRepo.tomarCupo(vueloId);
        if (afectados == 0) {
            throw new ApiException(409, "Ese vuelo ya no tiene cupos disponibles.");
        }

        Integer vueloAnteriorId = actual != null ? actual.getVuelo().getId() : null;
        if (actual == null) {
            actual = new Reserva();
            actual.setUsuarioId(usuarioId);
        }
        actual.setVuelo(destino);
        actual.setConsentimientoFirmado(false);
        actual.setCreadoEn(OffsetDateTime.now());
        reservaRepo.saveAndFlush(actual);

        if (vueloAnteriorId != null) {
            vueloRepo.liberarCupo(vueloAnteriorId);
        }
        entityManager.clear();
        return obtener(usuarioId).orElseThrow();
    }

    @Override
    @Transactional
    public ReservaDTO firmar(long usuarioId, boolean firmado) {
        Reserva actual = reservaRepo.findById(usuarioId)
                .orElseThrow(() -> new ApiException(404, "Aún no tienes una reserva."));
        actual.setConsentimientoFirmado(firmado);
        return ReservaDTO.desde(reservaRepo.save(actual));
    }

    @Override
    @Transactional
    public void cancelar(long usuarioId) {
        Reserva actual = reservaRepo.findById(usuarioId)
                .orElseThrow(() -> new ApiException(404, "No tienes una reserva para cancelar."));
        int vueloId = actual.getVuelo().getId();
        reservaRepo.delete(actual);
        vueloRepo.liberarCupo(vueloId);
    }

    @Override
    public List<Map<String, Object>> listarTodasAdmin() {
        List<Reserva> todas = reservaRepo.findAll();
        Map<Long, UsuarioPublicoDTO> usuariosPorId = obtenerUsuariosRemotos();

        List<Map<String, Object>> salida = new ArrayList<>();
        for (Reserva r : todas) {
            UsuarioPublicoDTO socio = usuariosPorId.get(r.getUsuarioId());
            Vuelo vuelo = r.getVuelo();
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
                .comparing((Map<String, Object> m) -> ((Vuelo) m.get("vuelo")).getFechaIso())
                .thenComparing(m -> ((Vuelo) m.get("vuelo")).getHorario()));
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
