package cl.vuelos.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.bouncycastle.crypto.generators.SCrypt;

public final class Contrasenas {
    private static final int N = 16384;
    private static final int R = 8;
    private static final int P = 1;
    private static final int LARGO = 64;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String FALSO = hash("contraseña-de-relleno-1");

    private Contrasenas() {}

    public static String hash(String contrasena) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        byte[] h = derivar(contrasena, sal);
        return "scrypt$" + HexFormat.of().formatHex(sal) + "$" + HexFormat.of().formatHex(h);
    }

    public static boolean verificar(String contrasena, String almacenado) {
        try {
            String[] partes = almacenado.split("\\$");
            if (partes.length != 3 || !partes[0].equals("scrypt")) return false;
            byte[] sal = HexFormat.of().parseHex(partes[1]);
            byte[] esperado = HexFormat.of().parseHex(partes[2]);
            return MessageDigest.isEqual(esperado, derivar(contrasena, sal));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void verificarFalso(String contrasena) {
        verificar(contrasena, FALSO);
    }

    private static byte[] derivar(String contrasena, byte[] sal) {
        return SCrypt.generate(contrasena.getBytes(StandardCharsets.UTF_8), sal, N, R, P, LARGO);
    }
}
