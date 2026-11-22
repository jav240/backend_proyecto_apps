package cl.vuelos.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operacion_vuelo")
@JsonPropertyOrder({"vueloId", "horometroInicial", "horometroFinal", "tripulantes", "horasVuelo", "actualizadoEn"})
public class OperacionVuelo {

    @Id
    @Column(name = "vuelo_id")
    private Integer vueloId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vuelo_id")
    private Vuelo vuelo;

    @Column(name = "horometro_inicial")
    private Double horometroInicial;

    @Column(name = "horometro_final")
    private Double horometroFinal;

    @Column(nullable = false)
    private int tripulantes = 0;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @JsonProperty("horasVuelo")
    public Double getHorasVuelo() {
        if (horometroInicial == null || horometroFinal == null) return null;
        return Math.round((horometroFinal - horometroInicial) * 100.0) / 100.0;
    }
}
