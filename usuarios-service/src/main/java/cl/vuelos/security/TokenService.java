package cl.vuelos.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] secreto;
    private final long ttlSegundos;

    public TokenService(@Value("${app.jwt-secret:}") String secreto,
                        @Value("${app.token-ttl-seconds:604800}") long ttlSegundos) {
        if (secreto == null || secreto.isBlank()) {
            byte[] azar = new byte[32];
            new SecureRandom().nextBytes(azar);
            secreto = HexFormat.of().formatHex(azar);
            log.warn("JWT_SECRET no está definido: se usa un secreto aleatorio y las sesiones se pierden al reiniciar.");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.ttlSegundos = ttlSegundos;
    }

    public String emitir(long id, String email) {
        return emitir(id, email, Roles.SOCIO);
    }

    public String emitir(long id, String email, String rol) {
        try {
            String cabecera = B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            Map<String, Object> carga = new LinkedHashMap<>();
            carga.put("id", id);
            carga.put("email", email);
            carga.put("rol", rol != null ? rol : Roles.SOCIO);
            carga.put("exp", Instant.now().getEpochSecond() + ttlSegundos);
            String cuerpo = B64.encodeToString(mapper.writeValueAsBytes(carga));
            String firma = B64.encodeToString(hmac(cabecera + "." + cuerpo));
            return cabecera + "." + cuerpo + "." + firma;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token.", e);
        }
    }

    public Optional<UsuarioToken> verificar(String token) {
        try {
            if (token == null) return Optional.empty();
            String[] partes = token.split("\\.", -1);
            if (partes.length != 3) return Optional.empty();

            byte[] esperada = hmac(partes[0] + "." + partes[1]);
            byte[] recibida = B64_DEC.decode(partes[2]);
            if (!MessageDigest.isEqual(esperada, recibida)) return Optional.empty();

            JsonNode cabecera = mapper.readTree(B64_DEC.decode(partes[0]));
            if (!"HS256".equals(cabecera.path("alg").asText())) return Optional.empty();

            JsonNode carga = mapper.readTree(B64_DEC.decode(partes[1]));
            if (carga.path("exp").asLong(0) <= Instant.now().getEpochSecond()) return Optional.empty();
            if (!carga.path("id").canConvertToLong() || !carga.path("email").isTextual()) return Optional.empty();
            String rol = carga.path("rol").isTextual() ? carga.get("rol").asText() : Roles.SOCIO;
            return Optional.of(new UsuarioToken(carga.get("id").asLong(), carga.get("email").asText(), rol));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private byte[] hmac(String texto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
        return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
    }
}
