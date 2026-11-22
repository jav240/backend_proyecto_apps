package cl.vuelos.config;

import cl.vuelos.entities.Vuelo;
import cl.vuelos.repository.VueloRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final VueloRepository vueloRepo;

    public DataInitializer(VueloRepository vueloRepo) {
        this.vueloRepo = vueloRepo;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (vueloRepo.count() > 0) return;
        List<Vuelo> vuelos = List.of(
                new Vuelo(null, "Tobalaba", "2026-11-07", "Sáb 7 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Ficticio Uno", 2),
                new Vuelo(null, "Tobalaba", "2026-11-07", "Sáb 7 nov 2026", "11:00 - 12:00", "Piper PA-28 (CC-BBB)", "Piloto Ficticio Dos", 3),
                new Vuelo(null, "Talcahuano", "2026-11-14", "Sáb 14 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Ficticio Uno", 1),
                new Vuelo(null, "Tobalaba", "2026-11-21", "Sáb 21 nov 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Ficticio Dos", 2),
                new Vuelo(null, "Talcahuano", "2026-11-28", "Sáb 28 nov 2026", "11:00 - 12:00", "Piper PA-28 (CC-BBB)", "Piloto Ficticio Uno", 3),
                new Vuelo(null, "Tobalaba", "2026-12-05", "Sáb 5 dic 2026", "10:00 - 11:00", "Cessna 172 (CC-AAA)", "Piloto Ficticio Uno", 2)
        );
        vueloRepo.saveAll(vuelos);
        log.info("Se cargaron {} vuelos de ejemplo en vuelos-operacion-service.", vuelos.size());
    }
}
