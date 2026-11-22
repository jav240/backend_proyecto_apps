package cl.vuelos.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LimiteIntentos {
    public static final int MAXIMO = 8;
    static final long VENTANA_MS = 10 * 60 * 1000L;
    private static final int MAX_CLAVES = 10_000;

    private final Map<String, Deque<Long>> fallos = new ConcurrentHashMap<>();

    public boolean bloqueado(String clave) {
        Deque<Long> cola = fallos.get(clave);
        if (cola == null) return false;
        synchronized (cola) {
            purgar(cola);
            return cola.size() >= MAXIMO;
        }
    }

    public void fallo(String clave) {
        if (fallos.size() > MAX_CLAVES) fallos.clear();
        Deque<Long> cola = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            purgar(cola);
            cola.addLast(System.currentTimeMillis());
        }
    }

    public void exito(String clave) {
        fallos.remove(clave);
    }

    private static void purgar(Deque<Long> cola) {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (!cola.isEmpty() && cola.peekFirst() < limite) cola.removeFirst();
    }
}
