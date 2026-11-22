package cl.vuelos.security;

public final class Roles {
    public static final String ADMINISTRADOR = "administrador";
    public static final String SOCIO = "socio";

    private Roles() {}

    public static boolean valido(String rol) {
        return ADMINISTRADOR.equals(rol) || SOCIO.equals(rol);
    }
}
