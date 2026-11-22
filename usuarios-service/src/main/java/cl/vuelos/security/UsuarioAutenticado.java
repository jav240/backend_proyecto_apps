package cl.vuelos.security;

import cl.vuelos.exception.ApiException;

public record UsuarioAutenticado(long id, String email, String rol) {

    public boolean esAdmin() {
        return Roles.ADMINISTRADOR.equals(rol);
    }

    public boolean esSocio() {
        return Roles.SOCIO.equals(rol);
    }

    public void exigirAdmin() {
        if (!esAdmin()) throw new ApiException(403, "Solo para administradores.");
    }

    public void exigirSocio() {
        if (!esSocio()) throw new ApiException(403, "Solo los socios reservan vuelos.");
    }
}
