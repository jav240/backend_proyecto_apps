package cl.vuelos.services;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.OperacionVueloRepository;
import cl.vuelos.repository.PasajeroRepository;
import cl.vuelos.repository.ReservaRepository;
import cl.vuelos.repository.VueloRepository;
import cl.vuelos.util.Fechas;
import cl.vuelos.util.Validaciones;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VueloServiceImpl implements VueloService {

    private final VueloRepository vueloRepo;
    private final ReservaRepository reservaRepo;
    private final PasajeroRepository pasajeroRepo;
    private final OperacionVueloRepository operacionRepo;

    public VueloServiceImpl(VueloRepository vueloRepo,
                            ReservaRepository reservaRepo,
                            PasajeroRepository pasajeroRepo,
                            OperacionVueloRepository operacionRepo) {
        this.vueloRepo = vueloRepo;
        this.reservaRepo = reservaRepo;
        this.pasajeroRepo = pasajeroRepo;
        this.operacionRepo = operacionRepo;
    }

    private record DatosVuelo(String base, String fechaIso, String horario, String aeronave, String piloto, int cupos) {}

    @Override
    public List<Vuelo> listar(String fecha, String disponibles) {
        String fechaFiltro = null;
        if (fecha != null && !fecha.isBlank()) {
            fechaFiltro = fecha.trim();
            if (!Validaciones.fechaValida(fechaFiltro)) {
                throw new ApiException(400, "La fecha debe tener el formato AAAA-MM-DD.");
            }
        }
        boolean soloDisponibles = disponibles != null && (disponibles.equalsIgnoreCase("true") || disponibles.equals("1"));
        return vueloRepo.buscarConFiltros(fechaFiltro, soloDisponibles);
    }

    @Override
    public Vuelo obtenerPorId(int id) {
        return vueloRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "El vuelo no existe."));
    }

    @Override
    @Transactional
    public Vuelo crear(Map<String, Object> body) {
        DatosVuelo d = validar(body, 1);
        Vuelo v = new Vuelo();
        v.setBase(d.base());
        v.setFechaIso(d.fechaIso());
        v.setFechaTexto(Fechas.texto(LocalDate.parse(d.fechaIso())));
        v.setHorario(d.horario());
        v.setAeronave(d.aeronave());
        v.setPiloto(d.piloto());
        v.setCupos(d.cupos());
        return vueloRepo.save(v);
    }

    @Override
    @Transactional
    public Vuelo editar(int id, Map<String, Object> body) {
        Vuelo actual = obtenerPorId(id);
        DatosVuelo d = validar(body, 0);

        if (d.cupos() < actual.getCupos() && enUso(id)) {
            throw new ApiException(409, "No puedes reducir los cupos de un vuelo que ya tiene reservas o pasajeros.");
        }

        actual.setBase(d.base());
        actual.setFechaIso(d.fechaIso());
        actual.setFechaTexto(Fechas.texto(LocalDate.parse(d.fechaIso())));
        actual.setHorario(d.horario());
        actual.setAeronave(d.aeronave());
        actual.setPiloto(d.piloto());
        actual.setCupos(d.cupos());
        return vueloRepo.save(actual);
    }

    @Override
    @Transactional
    public void eliminar(int id) {
        Vuelo vuelo = obtenerPorId(id);
        if (enUso(id)) {
            throw new ApiException(409, "El vuelo ya tiene reservas o pasajeros y no se puede eliminar.");
        }
        operacionRepo.deleteById(id);
        vueloRepo.delete(vuelo);
    }

    private boolean enUso(int vueloId) {
        return reservaRepo.existsByVuelo_Id(vueloId) || pasajeroRepo.existsByVuelo_Id(vueloId);
    }

    private DatosVuelo validar(Map<String, Object> body, int cuposMinimos) {
        Map<String, String> campos = new LinkedHashMap<>();

        String base = Validaciones.base(Validaciones.texto(body.get("base")));
        if (base == null) campos.put("base", "La base debe ser Tobalaba o Talcahuano.");

        String fechaIso = Validaciones.texto(body.get("fechaIso")).trim();
        if (!Validaciones.fechaValida(fechaIso)) campos.put("fechaIso", "La fecha debe tener el formato AAAA-MM-DD.");

        String horario = Validaciones.texto(body.get("horario")).trim();
        if (!Validaciones.horarioValido(horario)) campos.put("horario", "El horario debe ser como 10:00 - 11:00.");

        String matricula = Validaciones.matricula(Validaciones.texto(body.get("matricula")));
        if (matricula == null) campos.put("matricula", "La matrícula no es válida (ej. CC-AAA).");

        String modelo = Validaciones.texto(body.get("modelo")).trim();
        if (modelo.isEmpty() || modelo.length() > 60) campos.put("modelo", "Ingresa el modelo de la aeronave.");

        String piloto = Validaciones.texto(body.get("piloto")).trim();
        if (piloto.length() < 3 || piloto.length() > 100) campos.put("piloto", "Ingresa el piloto al mando.");

        Double cupos = Validaciones.numero(body.get("cupos"));
        if (cupos == null || cupos != Math.rint(cupos) || cupos < cuposMinimos || cupos > 20) {
            campos.put("cupos", "Los cupos deben ser un entero entre " + cuposMinimos + " y 20.");
        }

        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        return new DatosVuelo(base, fechaIso, horario, modelo + " (" + matricula + ")", piloto, cupos.intValue());
    }
}
