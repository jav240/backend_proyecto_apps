package cl.vuelos.config;

import cl.vuelos.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth;
    private final String[] rutasProtegidas;

    public WebConfig(AuthInterceptor auth,
                     @Value("${app.rutas-protegidas:/reserva,/reserva/**,/operacion/**,/admin/**}") String[] rutasProtegidas) {
        this.auth = auth;
        this.rutasProtegidas = rutasProtegidas;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rutasProtegidas.length > 0) {
            registry.addInterceptor(auth).addPathPatterns(rutasProtegidas);
        }
    }
}
