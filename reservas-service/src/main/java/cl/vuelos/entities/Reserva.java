package cl.vuelos.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "vuelo_id", nullable = false)
    private Integer vueloId;

    @Column(name = "consentimiento_firmado", nullable = false)
    private boolean consentimientoFirmado = false;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @PrePersist
    void prePersist() {
        if (creadoEn == null) creadoEn = OffsetDateTime.now();
    }
}
