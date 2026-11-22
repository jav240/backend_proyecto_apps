package cl.vuelos.util;

import java.time.LocalDate;

public final class Fechas {
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};

    private Fechas() {}

    public static String texto(LocalDate fecha) {
        return DIAS[fecha.getDayOfWeek().getValue() - 1] + " " + fecha.getDayOfMonth() + " "
                + MESES[fecha.getMonthValue() - 1] + " " + fecha.getYear();
    }
}
