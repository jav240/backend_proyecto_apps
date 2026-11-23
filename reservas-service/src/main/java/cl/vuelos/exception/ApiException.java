package cl.vuelos.exception;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final int status;
    private final Map<String, String> campos;

    public ApiException(int status, String mensaje) {
        this(status, mensaje, Map.of());
    }

    public ApiException(int status, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.status = status;
        this.campos = campos;
    }

    public int status() {
        return status;
    }

    public Map<String, String> campos() {
        return campos;
    }
}
