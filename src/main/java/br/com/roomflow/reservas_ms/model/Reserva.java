package br.com.roomflow.reservas_ms.model;

import br.com.roomflow.reservas_ms.exception.ConflitoException;
import br.com.roomflow.reservas_ms.exception.ValidacaoException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="reservas")
@NoArgsConstructor
@Getter
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private StatusReserva statusReserva;
    private Long usuarioId;
    private Long salaId;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    public Reserva(Long salaId, Long usuarioId, LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (salaId == null || usuarioId == null) {
            throw new ValidacaoException(
                    "Sala e usuário são obrigatórios para criar uma reserva!"
            );
        }

        validarHorarios(dataInicio, dataFim);

        this.salaId = salaId;
        this.usuarioId = usuarioId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.statusReserva = StatusReserva.ATIVA;
    }

    public void marcarReservaComoCancelada() {
        if (this.statusReserva == StatusReserva.CANCELADA) {
            throw new ConflitoException("Reserva já está cancelada!");
        }

        if (!LocalDateTime.now().isBefore(this.dataFim)) {
            throw new ConflitoException("Não é possível cancelar uma reserva que já foi encerrada!");
        }

        if (!LocalDateTime.now().isBefore(this.dataInicio)) {
            throw new ConflitoException("Não é possível cancelar uma reserva que já começou!");
        }

        this.statusReserva = StatusReserva.CANCELADA;
    }

    private void validarHorarios(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new ValidacaoException("Data de início e data de fim são obrigatórias!");
        }

        if (!dataInicio.isBefore(dataFim)) {
            throw new ValidacaoException("A Data de Inicio deve ser antes da Data de Fim");
        }
    }
}
