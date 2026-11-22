package cl.vuelos.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException e) {
        return respuesta(e.status(), e.getMessage(), e.campos());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return respuesta(400, "El cuerpo de la solicitud no es un JSON válido.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Map<String, Object>> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return respuesta(415, "Envía el cuerpo con Content-Type: application/json.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return respuesta(405, "Método no permitido.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> noEncontrada(NoResourceFoundException e) {
        return respuesta(404, "Ruta no encontrada.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return respuesta(400, "El parámetro '" + e.getName() + "' no es válido.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> conflicto(DataIntegrityViolationException e) {
        log.warn("Conflicto de integridad: {}", e.getMostSpecificCause().getMessage());
        return respuesta(409, "La operación entra en conflicto con datos existentes.", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return respuesta(500, "Error interno del servidor.", null);
    }

    private static ResponseEntity<Map<String, Object>> respuesta(int status, String error, Map<String, String> campos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", error);
        if (campos != null && !campos.isEmpty()) cuerpo.put("campos", campos);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(cuerpo);
    }
}
