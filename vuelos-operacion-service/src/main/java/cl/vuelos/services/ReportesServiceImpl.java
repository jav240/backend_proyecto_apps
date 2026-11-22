package cl.vuelos.services;

import cl.vuelos.entities.OperacionVuelo;
import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.OperacionVueloRepository;
import cl.vuelos.repository.PasajeroRepository;
import cl.vuelos.repository.VueloRepository;
import cl.vuelos.util.Validaciones;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportesServiceImpl implements ReportesService {

    private final VueloRepository vueloRepo;
    private final OperacionVueloRepository operacionRepo;
    private final PasajeroRepository pasajeroRepo;

    public ReportesServiceImpl(VueloRepository vueloRepo,
                               OperacionVueloRepository operacionRepo,
                               PasajeroRepository pasajeroRepo) {
        this.vueloRepo = vueloRepo;
        this.operacionRepo = operacionRepo;
        this.pasajeroRepo = pasajeroRepo;
    }

    @Override
    public Map<String, Object> estadisticas(String desde, String hasta) {
        String d = validarFecha(desde, "desde");
        String h = validarFecha(hasta, "hasta");
        List<Vuelo> vuelos = vueloRepo.buscarPorRango(d, h);
        List<Integer> ids = vuelos.stream().map(Vuelo::getId).toList();
        Map<Integer, OperacionVuelo> operaciones = ids.isEmpty() ? Map.of() :
                operacionRepo.findByVueloIdIn(ids).stream()
                        .collect(Collectors.toMap(OperacionVuelo::getVueloId, Function.identity()));

        long ejecutados = 0;
        double horas = 0;
        Set<String> pilotos = new HashSet<>();
        Set<String> aeronaves = new HashSet<>();

        for (Vuelo v : vuelos) {
            OperacionVuelo o = operaciones.get(v.getId());
            if (o == null || o.getHorometroInicial() == null || o.getHorometroFinal() == null) continue;
            ejecutados++;
            horas += o.getHorometroFinal() - o.getHorometroInicial();
            pilotos.add(v.getPiloto());
            aeronaves.add(v.getAeronave());
        }

        long personasTotal = ids.isEmpty() ? 0L : pasajeroRepo.contarPersonasDistintas(ids);
        long socioClub = ids.isEmpty() ? 0L : pasajeroRepo.contarPersonasDistintasPorCategoria(ids, "SOCIO_CLUB");
        long socioBienestar = ids.isEmpty() ? 0L : pasajeroRepo.contarPersonasDistintasPorCategoria(ids, "SOCIO_BIENESTAR");

        Map<String, Object> porCategoria = new LinkedHashMap<>();
        porCategoria.put("SOCIO_CLUB", socioClub);
        porCategoria.put("SOCIO_BIENESTAR", socioBienestar);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("desde", d);
        r.put("hasta", h);
        r.put("vuelosEjecutados", ejecutados);
        r.put("personasBeneficiadas", personasTotal);
        r.put("personasPorCategoria", porCategoria);
        r.put("pilotosParticipantes", (long) pilotos.size());
        r.put("aeronavesUtilizadas", (long) aeronaves.size());
        r.put("horasVueloTotales", Math.round(horas * 100.0) / 100.0);
        return r;
    }

    @Override
    public String generarCsv(String desde, String hasta) {
        String d = validarFecha(desde, "desde");
        String h = validarFecha(hasta, "hasta");
        List<Vuelo> vuelos = vueloRepo.buscarPorRango(d, h);
        List<Integer> ids = vuelos.stream().map(Vuelo::getId).toList();
        Map<Integer, OperacionVuelo> operaciones = ids.isEmpty() ? Map.of() :
                operacionRepo.findByVueloIdIn(ids).stream()
                        .collect(Collectors.toMap(OperacionVuelo::getVueloId, Function.identity()));

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("vueloId,fecha,horario,base,aeronave,piloto,tripulantes,pasajerosAbordo,")
                .append("horometroInicial,horometroFinal,horasVuelo,estado\r\n");

        for (Vuelo v : vuelos) {
            int id = v.getId();
            OperacionVuelo o = operaciones.get(id);
            boolean hayIni = o != null && o.getHorometroInicial() != null;
            boolean hayFin = o != null && o.getHorometroFinal() != null;
            double ini = hayIni ? o.getHorometroInicial() : 0;
            double fin = hayFin ? o.getHorometroFinal() : 0;
            long pasajeros = pasajeroRepo.contarAbordoPorVuelo(id);

            sb.append(id).append(',')
                    .append(celda(v.getFechaIso())).append(',')
                    .append(celda(v.getHorario())).append(',')
                    .append(celda(v.getBase())).append(',')
                    .append(celda(v.getAeronave())).append(',')
                    .append(celda(v.getPiloto())).append(',')
                    .append(o == null ? 0 : o.getTripulantes()).append(',')
                    .append(pasajeros).append(',')
                    .append(hayIni ? String.valueOf(ini) : "").append(',')
                    .append(hayFin ? String.valueOf(fin) : "").append(',')
                    .append(hayIni && hayFin ? String.valueOf(Math.round((fin - ini) * 100.0) / 100.0) : "")
                    .append(',')
                    .append(hayIni && hayFin ? "ejecutado" : "programado")
                    .append("\r\n");
        }
        return sb.toString();
    }

    private static String validarFecha(String valor, String nombre) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim();
        if (!Validaciones.fechaValida(v)) throw new ApiException(400, nombre + " debe tener el formato AAAA-MM-DD.");
        return v;
    }

    private static String celda(String texto) {
        if (texto == null) return "";
        String v = texto;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
