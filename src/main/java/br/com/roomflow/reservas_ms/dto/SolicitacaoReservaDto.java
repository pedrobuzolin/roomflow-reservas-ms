package br.com.roomflow.reservas_ms.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SolicitacaoReservaDto(@NotNull Long salaId,
                                    @NotNull Long usuarioId,
                                    @NotNull LocalDateTime dataInicio,
                                    @NotNull LocalDateTime dataFim) {
}
