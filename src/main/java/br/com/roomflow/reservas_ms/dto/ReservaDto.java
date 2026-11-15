package br.com.roomflow.reservas_ms.dto;

import br.com.roomflow.reservas_ms.model.Reserva;
import br.com.roomflow.reservas_ms.model.StatusReserva;

import java.time.LocalDateTime;

public record ReservaDto(Long id, Long salaId, Long usuarioId, LocalDateTime dataInicio, LocalDateTime dataFim, StatusReserva statusReserva) {
    public ReservaDto(Reserva reserva) {this(reserva.getId(), reserva.getSalaId(), reserva.getUsuarioId(),
            reserva.getDataInicio(), reserva.getDataFim(), reserva.getStatusReserva());}
}