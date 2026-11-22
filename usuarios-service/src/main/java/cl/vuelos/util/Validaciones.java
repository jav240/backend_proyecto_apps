package cl.vuelos.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Validaciones {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern FECHA = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern HORARIO =
            Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d - ([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern MATRICULA = Pattern.compile("^[A-Z0-9]{1,3}-[A-Z0-9]{2,5}$");

    private Validaciones() {}

    public static String texto(Object valor) {
        return valor instanceof String s ? s : "";
    }

    public static String nombre(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) return "Ingresa tu nombre completo.";
        if (v.length() < 3) return "El nombre es muy corto.";
        if (!v.contains(" ")) return "Ingresa nombre y apellido.";
        if (v.length() > 100) return "El nombre es demasiado largo.";
        return null;
    }

    public static String email(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) return "Ingresa tu correo.";
        if (v.length() > 254 || !EMAIL.matcher(v).matches()) return "El correo no es válido.";
        return null;
    }

    public static String password(String valor) {
        String v = valor == null ? "" : valor;
        if (v.isBlank()) return "Ingresa una contraseña.";
        if (v.length() < 6) return "Debe tener al menos 6 caracteres.";
        if (v.chars().noneMatch(Character::isDigit)) return "Debe incluir al menos un número.";
        if (v.length() > 128) return "La contraseña es demasiado larga.";
        return null;
    }

    public static boolean fechaValida(String valor) {
        if (valor == null || !FECHA.matcher(valor).matches()) return false;
        try {
            LocalDate.parse(valor);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String normalizarRut(String valor) {
        if (valor == null) return null;
        String v = valor.replace(".", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (v.length() < 2) return null;
        String cuerpo;
        String dv;
        int guion = v.indexOf('-');
        if (guion >= 0) {
            cuerpo = v.substring(0, guion);
            dv = v.substring(guion + 1);
        } else {
            cuerpo = v.substring(0, v.length() - 1);
            dv = v.substring(v.length() - 1);
        }
        if (!cuerpo.matches("\\d{7,8}") || !dv.matches("[0-9K]")) return null;
        int suma = 0;
        int multiplo = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplo;
            multiplo = multiplo == 7 ? 2 : multiplo + 1;
        }
        int resto = 11 - (suma % 11);
        String esperado = resto == 11 ? "0" : resto == 10 ? "K" : String.valueOf(resto);
        return esperado.equals(dv) ? cuerpo + "-" + dv : null;
    }

    public static String categoria(String valor) {
        if (valor == null) return null;
        String v = valor.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return v.equals("SOCIO_CLUB") || v.equals("SOCIO_BIENESTAR") ? v : null;
    }

    public static String nombrePasajero(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.length() < 3) return "Ingresa el nombre completo del pasajero.";
        if (v.length() > 100) return "El nombre es demasiado largo.";
        return null;
    }

    public static Double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : null;
    }

    public static String peso(Double kg) {
        if (kg == null) return "El peso debe ser un número en kilogramos.";
        if (kg < 10 || kg > 250) return "El peso debe estar entre 10 y 250 kg.";
        return null;
    }

    public static String horometro(Double horas) {
        if (horas == null) return "El horómetro debe ser un número.";
        if (horas < 0 || horas > 999_999) return "El horómetro debe estar entre 0 y 999999.";
        return null;
    }

    public static boolean horarioValido(String valor) {
        return valor != null && HORARIO.matcher(valor).matches();
    }

    public static String matricula(String valor) {
        String v = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        return MATRICULA.matcher(v).matches() ? v : null;
    }

    public static String base(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.equalsIgnoreCase("Tobalaba")) return "Tobalaba";
        if (v.equalsIgnoreCase("Talcahuano")) return "Talcahuano";
        return null;
    }
}
