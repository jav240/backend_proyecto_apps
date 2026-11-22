package cl.vuelos.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vuelos")
@JsonPropertyOrder({"id", "base", "fechaIso", "fechaTexto", "horario", "aeronave", "piloto", "cupos"})
public class Vuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String base;

    @Column(name = "fecha_iso", nullable = false)
    private String fechaIso;

    @Column(name = "fecha_texto", nullable = false)
    private String fechaTexto;

    @Column(nullable = false)
    private String horario;

    @Column(nullable = false)
    private String aeronave;

    @Column(nullable = false)
    private String piloto;

    @Column(nullable = false)
    private Integer cupos;
}
