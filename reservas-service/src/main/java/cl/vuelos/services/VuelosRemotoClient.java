package cl.vuelos.services;

import cl.vuelos.dto.VueloDTO;
import cl.vuelos.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VuelosRemotoClient {
    private static final Logger log = LoggerFactory.getLogger(VuelosRemotoClient.class);

    private final String vuelosServiceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public VuelosRemotoClient(@Value("${app.vuelos-service-url:http://localhost:8082}") String vuelosServiceUrl) {
        this.vuelosServiceUrl = vuelosServiceUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public VueloDTO obtenerVuelo(int vueloId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(vuelosServiceUrl + "/internal/vuelos/" + vueloId))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404) {
                throw new ApiException(404, "El vuelo no existe.");
            }
            if (res.statusCode() == 200) {
                return mapper.readValue(res.body(), VueloDTO.class);
            }
            throw new ApiException(502, "Error al consultar vuelos-operacion-service.");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error comunicando con vuelos-operacion-service: {}", e.getMessage());
            throw new ApiException(503, "Servicio de vuelos no disponible.");
        }
    }

    public Map<Integer, VueloDTO> obtenerTodosPorId() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(vuelosServiceUrl + "/internal/vuelos"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                List<VueloDTO> lista = mapper.readValue(res.body(), new TypeReference<>() {});
                return lista.stream().collect(Collectors.toMap(VueloDTO::id, Function.identity()));
            }
        } catch (Exception e) {
            log.warn("No se pudo listar vuelos remotos: {}", e.getMessage());
        }
        return Map.of();
    }

    public void tomarCupo(int vueloId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(vuelosServiceUrl + "/internal/vuelos/" + vueloId + "/tomar-cupo"))
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 409) {
                throw new ApiException(409, "Ese vuelo ya no tiene cupos disponibles.");
            }
            if (res.statusCode() == 404) {
                throw new ApiException(404, "El vuelo no existe.");
            }
            if (res.statusCode() != 200) {
                throw new ApiException(502, "No se pudo reservar cupo en el vuelo.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(503, "Servicio de vuelos no disponible.");
        }
    }

    public void liberarCupo(int vueloId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(vuelosServiceUrl + "/internal/vuelos/" + vueloId + "/liberar-cupo"))
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("No se pudo liberar cupo en vuelo {}: {}", vueloId, e.getMessage());
        }
    }
}
