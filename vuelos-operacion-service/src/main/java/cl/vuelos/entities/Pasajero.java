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
@Table(name = "pasajeros", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"vuelo_id", "cedula"})
})
@JsonPropertyOrder({"id", "idCliente", "vueloId", "cedula", "nombre", "categoria", "pesoKg",
        "consentimientoFirmado", "abordo", "creadoEn"})
public class Pasajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_cliente", unique = true)
    private String idCliente;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @Column(nullable = false)
    private String cedula;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String categoria;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "consentimiento_firmado", nullable = false)
    private boolean consentimientoFirmado = false;

    @Column(nullable = false)
    private boolean abordo = false;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @JsonProperty("vueloId")
    public Integer getVueloId() {
        return vuelo != null ? vuelo.getId() : null;
    }

    @PrePersist
    void prePersist() {
        if (creadoEn == null) creadoEn = OffsetDateTime.now();
    }
}
