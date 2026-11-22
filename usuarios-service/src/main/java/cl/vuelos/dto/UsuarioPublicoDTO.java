package cl.vuelos.dto;

import cl.vuelos.entities.Usuario;

public record UsuarioPublicoDTO(long id, String nombre, String email, String rol) {
    public static UsuarioPublicoDTO desde(Usuario u) {
        return new UsuarioPublicoDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
    }
}
