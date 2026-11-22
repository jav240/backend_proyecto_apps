package cl.vuelos.security;

import cl.vuelos.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USUARIO = "usuarioAutenticado";
    private static final String PREFIJO = "Bearer ";
    private static final String SESION_INVALIDA = "Tu sesión no es válida o expiró. Inicia sesión otra vez.";

    private final TokenService tokens;

    public AuthInterceptor(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            throw new ApiException(401, "Debes iniciar sesión.");
        }
        UsuarioToken tokenData = tokens.verificar(cabecera.substring(PREFIJO.length()).trim())
                .orElseThrow(() -> new ApiException(401, SESION_INVALIDA));

        request.setAttribute(USUARIO, new UsuarioAutenticado(tokenData.id(), tokenData.email(), tokenData.rol()));
        return true;
    }
}
