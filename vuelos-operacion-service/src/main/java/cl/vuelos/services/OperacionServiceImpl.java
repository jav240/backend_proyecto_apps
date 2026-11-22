package cl.vuelos.services;

import cl.vuelos.entities.OperacionVuelo;
import cl.vuelos.entities.Pasajero;
import cl.vuelos.entities.Vuelo;
import cl.vuelos.exception.ApiException;
import cl.vuelos.repository.OperacionVueloRepository;
import cl.vuelos.repository.PasajeroRepository;
import cl.vuelos.repository.VueloRepository;
import cl.vuelos.util.Validaciones;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacionServiceImpl implements OperacionService {

    private static final int MAX_SINCRONIZACION = 500;

    private final VueloRepository vueloRepo;
    private final PasajeroRepository pasajeroRepo;
    private final OperacionVueloRepository operacionRepo;

    public OperacionServiceImpl(VueloRepository vueloRepo,
                                PasajeroRepository pasajeroRepo,
                                OperacionVueloRepository operacionRepo) {
        this.vueloRepo = vueloRepo;
        this.pasajeroRepo = pasajeroRepo;
        this.operacionRepo = operacionRepo;
    }

    private Vuelo exigirVuelo(int vueloId) {
        return vueloRepo.findById(vueloId)
                .orElseThrow(() -> new ApiException(404, "El vuelo no existe."));
    }

    @Override
    public List<Pasajero> pasajeros(int vueloId) {
        exigirVuelo(vueloId);
        return pasajeroRepo.findByVuelo_IdOrderByNombreAscIdAsc(vueloId);
    }

    @Override
    @Transactional
    public RegistroPasajero registrar(int vueloId, Map<String, Object> body) {
        Vuelo vuelo = exigirVuelo(vueloId);

        String idCliente = Validaciones.texto(body.get("idCliente")).trim();
        if (idCliente.length() > 64) {
            throw new ApiException(400, "Revisa los campos marcados.", Map.of("idCliente", "Máximo 64 caracteres."));
        }
        if (!idCliente.isEmpty()) {
            Pasajero existente = pasajeroRepo.findByIdCliente(idCliente).orElse(null);
            if (existente != null) {
                if (!existente.getVuelo().getId().equals(vueloId)) {
                    throw new ApiException(409, "Ese idCliente ya está registrado en otro vuelo.",
                            Map.of("idCliente", "Ya está asociado a otro vuelo."));
                }
                boolean hayCambios = body.containsKey("pesoKg") || body.containsKey("consentimientoFirmado")
                        || body.containsKey("abordo");
                return new RegistroPasajero(hayCambios ? actualizar(existente.getId(), body) : existente, false);
            }
        }

        Map<String, String> campos = new LinkedHashMap<>();
        String cedula = Validaciones.normalizarRut(Validaciones.texto(body.get("cedula")));
        if (cedula == null) campos.put("cedula", "La cédula no es válida (ej. 12345678-5).");

        String nombre = Validaciones.texto(body.get("nombre")).trim();
        String errorNombre = Validaciones.nombrePasajero(nombre);
        if (errorNombre != null) campos.put("nombre", errorNombre);

        String categoria = Validaciones.categoria(Validaciones.texto(body.get("categoria")));
        if (categoria == null) campos.put("categoria", "La categoría debe ser SOCIO_CLUB o SOCIO_BIENESTAR.");

        Double peso = null;
        if (body.get("pesoKg") != null) {
            peso = Validaciones.numero(body.get("pesoKg"));
            String errorPeso = Validaciones.peso(peso);
            if (errorPeso != null) campos.put("pesoKg", errorPeso);
        }

        boolean firmado = false;
        Object valorFirma = body.get("consentimientoFirmado");
        if (valorFirma != null) {
            if (valorFirma instanceof Boolean b) firmado = b;
            else campos.put("consentimientoFirmado", "Debe ser true o false.");
        }

        boolean abordo = false;
        Object valorAbordo = body.get("abordo");
        if (valorAbordo != null) {
            if (valorAbordo instanceof Boolean b) abordo = b;
            else campos.put("abordo", "Debe ser true o false.");
        }
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);
        exigirRequisitosDeAbordaje(abordo, peso, firmado);

        if (pasajeroRepo.existsByVuelo_IdAndCedula(vueloId, cedula)) {
            throw new ApiException(409, "Ese pasajero ya está registrado en el vuelo.",
                    Map.of("cedula", "Ya está registrado en este vuelo."));
        }

        Pasajero nuevo = new Pasajero();
        nuevo.setIdCliente(idCliente.isEmpty() ? null : idCliente);
        nuevo.setVuelo(vuelo);
        nuevo.setCedula(cedula);
        nuevo.setNombre(nombre);
        nuevo.setCategoria(categoria);
        nuevo.setPesoKg(peso);
        nuevo.setConsentimientoFirmado(firmado);
        nuevo.setAbordo(abordo);
        nuevo = pasajeroRepo.save(nuevo);
        return new RegistroPasajero(nuevo, true);
    }

    @Override
    @Transactional
    public Pasajero actualizar(long id, Map<String, Object> body) {
        Pasajero actual = pasajeroRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "El pasajero no existe."));

        if (!body.containsKey("pesoKg") && !body.containsKey("consentimientoFirmado") && !body.containsKey("abordo")) {
            throw new ApiException(400, "Envía pesoKg, consentimientoFirmado o abordo.");
        }

        Double peso = actual.getPesoKg();
        boolean firmado = actual.isConsentimientoFirmado();
        boolean abordo = actual.isAbordo();
        Map<String, String> campos = new LinkedHashMap<>();

        if (body.containsKey("pesoKg")) {
            Double nuevo = Validaciones.numero(body.get("pesoKg"));
            String error = Validaciones.peso(nuevo);
            if (error != null) campos.put("pesoKg", error);
            else peso = nuevo;
        }
        if (body.containsKey("consentimientoFirmado")) {
            if (body.get("consentimientoFirmado") instanceof Boolean b) firmado = b;
            else campos.put("consentimientoFirmado", "Debe ser true o false.");
        }
        if (body.containsKey("abordo")) {
            if (body.get("abordo") instanceof Boolean b) abordo = b;
            else campos.put("abordo", "Debe ser true o false.");
        }
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);
        exigirRequisitosDeAbordaje(abordo, peso, firmado);

        actual.setPesoKg(peso);
        actual.setConsentimientoFirmado(firmado);
        actual.setAbordo(abordo);
        return pasajeroRepo.save(actual);
    }

    @Override
    @Transactional
    public void eliminar(long id) {
        Pasajero actual = pasajeroRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "El pasajero no existe."));
        if (actual.isAbordo()) {
            throw new ApiException(409, "El pasajero ya abordó y no se puede eliminar.");
        }
        pasajeroRepo.delete(actual);
    }

    private static void exigirRequisitosDeAbordaje(boolean abordo, Double peso, boolean firmado) {
        if (abordo && (peso == null || !firmado)) {
            throw new ApiException(409, "Para abordar se necesita el peso verificado y el consentimiento firmado.");
        }
    }

    @Override
    public OperacionVuelo operacion(int vueloId) {
        Vuelo vuelo = exigirVuelo(vueloId);
        return operacionRepo.findById(vueloId).orElseGet(() -> {
            OperacionVuelo vacia = new OperacionVuelo();
            vacia.setVueloId(vuelo.getId());
            vacia.setVuelo(vuelo);
            vacia.setTripulantes(0);
            return vacia;
        });
    }

    @Override
    @Transactional
    public OperacionVuelo guardarOperacion(int vueloId, Map<String, Object> body) {
        Vuelo vuelo = exigirVuelo(vueloId);
        OperacionVuelo actual = operacionRepo.findById(vueloId).orElseGet(() -> {
            OperacionVuelo nueva = new OperacionVuelo();
            nueva.setVuelo(vuelo);
            nueva.setTripulantes(0);
            return nueva;
        });

        boolean hayInicial = body.containsKey("horometroInicial");
        boolean hayFinal = body.containsKey("horometroFinal");
        boolean hayTripulantes = body.containsKey("tripulantes");
        if (!hayInicial && !hayFinal && !hayTripulantes) {
            throw new ApiException(400, "Envía horometroInicial, horometroFinal o tripulantes.");
        }

        Double inicial = actual.getHorometroInicial();
        Double fin = actual.getHorometroFinal();
        int tripulantes = actual.getTripulantes();
        Map<String, String> campos = new LinkedHashMap<>();

        if (hayInicial) {
            Double v = Validaciones.numero(body.get("horometroInicial"));
            String error = Validaciones.horometro(v);
            if (error != null) campos.put("horometroInicial", error);
            else inicial = v;
        }
        if (hayFinal) {
            Double v = Validaciones.numero(body.get("horometroFinal"));
            String error = Validaciones.horometro(v);
            if (error != null) campos.put("horometroFinal", error);
            else fin = v;
        }
        if (hayTripulantes) {
            if (body.get("tripulantes") instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())
                    && n.intValue() >= 0 && n.intValue() <= 20) {
                tripulantes = n.intValue();
            } else {
                campos.put("tripulantes", "Debe ser un número entero entre 0 y 20.");
            }
        }
        if (campos.isEmpty() && inicial != null && fin != null && fin < inicial) {
            campos.put("horometroFinal", "El horómetro final no puede ser menor que el inicial.");
        }
        if (!campos.isEmpty()) throw new ApiException(400, "Revisa los campos marcados.", campos);

        actual.setHorometroInicial(inicial);
        actual.setHorometroFinal(fin);
        actual.setTripulantes(tripulantes);
        actual.setActualizadoEn(OffsetDateTime.now());
        return operacionRepo.save(actual);
    }

    @Override
    public Map<String, Object> sincronizar(Map<String, Object> body) {
        List<Object> pasajeros = lista(body.get("pasajeros"));
        List<Object> horometros = lista(body.get("horometros"));
        if (pasajeros.size() > MAX_SINCRONIZACION || horometros.size() > MAX_SINCRONIZACION) {
            throw new ApiException(400, "Máximo " + MAX_SINCRONIZACION + " elementos por sincronización.");
        }
        int errores = 0;

        List<Map<String, Object>> resultadoPasajeros = new ArrayList<>();
        for (int i = 0; i < pasajeros.size(); i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("indice", i);
            try {
                Map<String, Object> item = mapa(pasajeros.get(i));
                r.put("idCliente", Validaciones.texto(item.get("idCliente")));
                RegistroPasajero registro = registrar(entero(item.get("vueloId")), item);
                r.put("estado", registro.creado() ? "creado" : "existente");
                r.put("pasajero", registro.pasajero());
            } catch (RuntimeException e) {
                errores++;
                ponerError(r, e);
            }
            resultadoPasajeros.add(r);
        }

        List<Map<String, Object>> resultadoHorometros = new ArrayList<>();
        for (int i = 0; i < horometros.size(); i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("indice", i);
            try {
                Map<String, Object> item = mapa(horometros.get(i));
                r.put("operacion", guardarOperacion(entero(item.get("vueloId")), item));
                r.put("estado", "ok");
            } catch (RuntimeException e) {
                errores++;
                ponerError(r, e);
            }
            resultadoHorometros.add(r);
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("pasajeros", resultadoPasajeros);
        respuesta.put("horometros", resultadoHorometros);
        respuesta.put("errores", errores);
        return respuesta;
    }

    private static void ponerError(Map<String, Object> r, RuntimeException e) {
        r.put("estado", "error");
        if (e instanceof ApiException a) {
            r.put("codigo", a.status());
            r.put("error", a.getMessage());
            if (!a.campos().isEmpty()) r.put("campos", a.campos());
        } else if (e instanceof DataAccessException) {
            r.put("codigo", 409);
            r.put("error", "La operación entra en conflicto con datos existentes.");
        } else {
            throw e;
        }
    }

    private static List<Object> lista(Object valor) {
        if (valor == null) return List.of();
        if (valor instanceof List<?> l) return new ArrayList<Object>(l);
        throw new ApiException(400, "El valor enviado debe ser una lista.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        if (valor instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new ApiException(400, "Cada elemento debe ser un objeto JSON.");
    }

    private static int entero(Object valor) {
        if (valor instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())
                && Math.abs(n.doubleValue()) <= Integer.MAX_VALUE) {
            return n.intValue();
        }
        throw new ApiException(400, "vueloId debe ser un número entero.");
    }
}
