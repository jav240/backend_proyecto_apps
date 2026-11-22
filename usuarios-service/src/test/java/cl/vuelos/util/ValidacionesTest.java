package cl.vuelos.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ValidacionesTest {

    @Test
    void nombreValidoEInvalido() {
        assertNull(Validaciones.nombre("Juan Pérez"));
        assertNotNull(Validaciones.nombre(""));
        assertNotNull(Validaciones.nombre("Ana"));
        assertNotNull(Validaciones.nombre("Juan")); // sin apellido
    }

    @Test
    void emailValidoEInvalido() {
        assertNull(Validaciones.email("socio@capebe.cl"));
        assertNotNull(Validaciones.email(""));
        assertNotNull(Validaciones.email("correo-invalido"));
    }

    @Test
    void passwordValidoEInvalido() {
        assertNull(Validaciones.password("Clave123"));
        assertNotNull(Validaciones.password("123")); // muy corta
        assertNotNull(Validaciones.password("sinnumeros")); // sin dígitos
    }

    @Test
    void normalizarRutChileno() {
        assertEquals("11111111-1", Validaciones.normalizarRut("11.111.111-1"));
        assertEquals("12345678-5", Validaciones.normalizarRut("123456785"));
        assertNull(Validaciones.normalizarRut("11.111.111-9")); // dígito verificador incorrecto
        assertNull(Validaciones.normalizarRut("abc"));
    }

    @Test
    void fechasHorariosYMatriculas() {
        assertTrue(Validaciones.fechaValida("2026-11-22"));
        assertFalse(Validaciones.fechaValida("22-11-2026"));
        assertFalse(Validaciones.fechaValida("2026-02-30"));

        assertTrue(Validaciones.horarioValido("10:00 - 11:00"));
        assertFalse(Validaciones.horarioValido("10:00 a 11:00"));

        assertEquals("CC-AAA", Validaciones.matricula("cc-aaa"));
        assertNull(Validaciones.matricula("INVALIDA"));

        assertEquals("Tobalaba", Validaciones.base("tobalaba"));
        assertEquals("Talcahuano", Validaciones.base("TALCAHUANO"));
        assertNull(Validaciones.base("Pudahuel"));
    }
}
