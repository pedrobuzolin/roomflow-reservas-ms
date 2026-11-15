package br.com.roomflow.reservas_ms.service;

import br.com.roomflow.reservas_ms.dto.ReservaDto;
import br.com.roomflow.reservas_ms.dto.SalaDto;
import br.com.roomflow.reservas_ms.dto.SolicitacaoReservaDto;
import br.com.roomflow.reservas_ms.exception.ConflitoException;
import br.com.roomflow.reservas_ms.exception.RecursoNaoEncontradoException;
import br.com.roomflow.reservas_ms.exception.ValidacaoException;
import br.com.roomflow.reservas_ms.http.SalaClient;
import br.com.roomflow.reservas_ms.http.UsuarioClient;
import br.com.roomflow.reservas_ms.model.Reserva;
import br.com.roomflow.reservas_ms.model.StatusReserva;
import br.com.roomflow.reservas_ms.repository.ReservaRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservaService {
    private static final String CONSTRAINT_SOBREPOSICAO = "reservas_sem_sobreposicao";

    private final ReservaRepository repository;
    private final SalaClient salaClient;
    private final UsuarioClient usuarioClient;

    public Long criarReserva(SolicitacaoReservaDto dto) {
        Reserva reserva = new Reserva(dto.salaId(), dto.usuarioId(), dto.dataInicio(), dto.dataFim());

        validarSala(dto.salaId());
        validarUsuario(dto.usuarioId());

        try {
            repository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            if (violouConstraintDeSobreposicao(e)) {
                throw new ConflitoException("A sala já está reservada neste período!");
            }
            throw e;
        }

        return reserva.getId();
    }

    @Transactional
    public void cancelarReserva(Long id) {
        Reserva reserva = buscarReserva(id);
        reserva.marcarReservaComoCancelada();
        repository.save(reserva);
    }

    @Transactional(readOnly = true)
    public Page<ReservaDto> listarTodasReservas(Pageable pageable) {
        return repository.findAll(pageable)
                .map(ReservaDto::new);
    }

    @Transactional(readOnly = true)
    public Page<ReservaDto> listarReservasAtivas(Pageable pageable) {
        return repository.findByStatusReserva(StatusReserva.ATIVA, pageable)
                .map(ReservaDto::new);
    }

    @Transactional(readOnly = true)
    public ReservaDto buscarReservaPorId(Long id) {
        return new ReservaDto(buscarReserva(id));
    }

    @Transactional(readOnly = true)
    public Page<ReservaDto> buscarReservasPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim, Pageable pageable) {
        if (!dataInicio.isBefore(dataFim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à data final!");
        }
        return repository.buscarPorPeriodo(dataInicio, dataFim, pageable)
                .map(ReservaDto::new);
    }

    public Page<SalaDto> listarSalasDisponiveis(LocalDateTime dataInicio, LocalDateTime dataFim, Pageable pageable) {
        if (!dataInicio.isBefore(dataFim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à data final!");
        }

        List<SalaDto> salasAtivas = salaClient.listarSalasAtivas();
        Set<Long> salasOcupadas = new HashSet<>(
                repository.buscarIdsSalasOcupadas(dataInicio, dataFim, StatusReserva.ATIVA));

        List<SalaDto> disponiveis = salasAtivas.stream()
                .filter(sala -> !salasOcupadas.contains(sala.id()))
                .toList();

        return paginar(disponiveis, pageable);
    }

    private void validarSala(Long id) {
        try {
            SalaDto sala = salaClient.verificaSala(id);
            if (!Boolean.TRUE.equals(sala.ativa())) {
                throw new ValidacaoException("Sala inativa!");
            }
        } catch (FeignException.NotFound e) {
            throw new RecursoNaoEncontradoException("Sala não encontrada!");
        }
    }

    private void validarUsuario(Long id) {
        try {
            usuarioClient.verificaUsuario(id);
        } catch (FeignException.NotFound e) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado!");
        }
    }

    private boolean violouConstraintDeSobreposicao(DataIntegrityViolationException e) {
        String mensagem = e.getMostSpecificCause().getMessage();
        return mensagem != null && mensagem.contains(CONSTRAINT_SOBREPOSICAO);
    }

    private Reserva buscarReserva(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada!"));
    }

    private <T> Page<T> paginar(List<T> lista, Pageable pageable) {
        int inicio = (int) Math.min(pageable.getOffset(), lista.size());
        int fim = Math.min(inicio + pageable.getPageSize(), lista.size());
        return new PageImpl<>(lista.subList(inicio, fim), pageable, lista.size());
    }
}
