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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {
    private static final LocalDateTime INICIO = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime FIM = LocalDateTime.now().plusHours(2);

    @Mock
    private ReservaRepository repository;
    @Mock
    private SalaClient salaClient;
    @Mock
    private UsuarioClient usuarioClient;

    @InjectMocks
    private ReservaService service;

    private Reserva criarReserva(Long id, Long salaId, Long usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        Reserva reserva = new Reserva(salaId, usuarioId, inicio, fim);
        setField(reserva, "id", id);
        return reserva;
    }

    private SalaDto criarSalaDto(Long id, boolean ativa) {
        return new SalaDto(id, "Sala " + id, 10, ativa);
    }

    private FeignException.NotFound naoEncontrado() {
        return mock(FeignException.NotFound.class);
    }

    @DisplayName("Quando criar uma reserva")
    @Nested
    class CriarReserva {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado uma reserva com dados validos")
            @Test
            void teste1() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenReturn(criarSalaDto(7L, true));
                when(repository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation -> {
                    Reserva reserva = invocation.getArgument(0);
                    setField(reserva, "id", 10L);
                    return reserva;
                });

                Long id = service.criarReserva(solicitacao);

                assertEquals(10L, id);
                ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
                verify(repository).saveAndFlush(captor.capture());
                Reserva reservaSalva = captor.getValue();
                assertEquals(7L, reservaSalva.getSalaId());
                assertEquals(3L, reservaSalva.getUsuarioId());
                assertEquals(INICIO, reservaSalva.getDataInicio());
                assertEquals(FIM, reservaSalva.getDataFim());
                assertEquals(StatusReserva.ATIVA, reservaSalva.getStatusReserva());
                verify(usuarioClient).verificaUsuario(3L);
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado uma sala inativa")
            @Test
            void teste1() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenReturn(criarSalaDto(7L, false));

                assertThrows(ValidacaoException.class, () -> service.criarReserva(solicitacao));
                verifyNoInteractions(usuarioClient, repository);
            }

            @DisplayName("Dado uma sala inexistente")
            @Test
            void teste2() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenThrow(naoEncontrado());

                assertThrows(RecursoNaoEncontradoException.class, () -> service.criarReserva(solicitacao));
                verifyNoInteractions(usuarioClient, repository);
            }

            @DisplayName("Dado um usuario inexistente ou inativo")
            @Test
            void teste3() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenReturn(criarSalaDto(7L, true));
                doThrow(naoEncontrado()).when(usuarioClient).verificaUsuario(3L);

                assertThrows(RecursoNaoEncontradoException.class, () -> service.criarReserva(solicitacao));
                verifyNoInteractions(repository);
            }

            @DisplayName("Dado um periodo invalido")
            @Test
            void teste4() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, FIM, INICIO);

                assertThrows(ValidacaoException.class, () -> service.criarReserva(solicitacao));

                verifyNoInteractions(salaClient, usuarioClient, repository);
            }

            @DisplayName("Dado conflito de horario na constraint do banco")
            @Test
            void teste5() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenReturn(criarSalaDto(7L, true));
                when(repository.saveAndFlush(any(Reserva.class))).thenThrow(
                        new DataIntegrityViolationException("erro",
                                new RuntimeException("conflicting key value violates exclusion constraint \"reservas_sem_sobreposicao\"")));

                assertThrows(ConflitoException.class, () -> service.criarReserva(solicitacao));
            }

            @DisplayName("Dado salas-ms fora do ar")
            @Test
            void teste6() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenThrow(mock(FeignException.class));

                assertThrows(FeignException.class, () -> service.criarReserva(solicitacao));

                verifyNoInteractions(usuarioClient, repository);
            }

            @DisplayName("Dado usuarios-ms fora do ar")
            @Test
            void teste7() {
                SolicitacaoReservaDto solicitacao = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(salaClient.verificaSala(7L)).thenReturn(criarSalaDto(7L, true));
                doThrow(mock(FeignException.class)).when(usuarioClient).verificaUsuario(3L);

                assertThrows(FeignException.class, () -> service.criarReserva(solicitacao));

                verifyNoInteractions(repository);
            }
        }
    }

    @DisplayName("Quando cancelar reserva")
    @Nested
    class CancelarReserva {
        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado um reserva valida")
            @Test
            void teste1() {
                Reserva reserva = criarReserva(1L, 7L, 3L, INICIO, FIM);
                when(repository.findById(1L)).thenReturn(Optional.of(reserva));

                service.cancelarReserva(1L);

                assertEquals(StatusReserva.CANCELADA, reserva.getStatusReserva());
                verify(repository).save(reserva);
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado uma reserva inexistente")
            @Test
            void teste1() {
                when(repository.findById(1L)).thenReturn(Optional.empty());

                assertThrows(RecursoNaoEncontradoException.class, () -> service.cancelarReserva(1L));

                verify(repository, never()).save(any(Reserva.class));
            }

            @DisplayName("Dado uma reserva ja cancelada")
            @Test
            void teste2() {
                Reserva reserva = criarReserva(1L, 7L, 3L, INICIO, FIM);
                setField(reserva, "statusReserva", StatusReserva.CANCELADA);
                when(repository.findById(1L)).thenReturn(Optional.of(reserva));

                assertThrows(ConflitoException.class, () -> service.cancelarReserva(1L));

                verify(repository, never()).save(any(Reserva.class));
            }

            @DisplayName("Dado uma reserva em andamento")
            @Test
            void teste3() {
                LocalDateTime agora = LocalDateTime.now();
                Reserva reserva = criarReserva(1L, 7L, 3L, agora.minusHours(1), agora.plusHours(1));
                when(repository.findById(1L)).thenReturn(Optional.of(reserva));

                assertThrows(ConflitoException.class, () -> service.cancelarReserva(1L));
                assertEquals(StatusReserva.ATIVA, reserva.getStatusReserva());
                verify(repository, never()).save(any(Reserva.class));
            }
        }
    }

    @DisplayName("Quando buscar reserva por id")
    @Nested
    class BuscarPorId {
        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado um id existente")
            @Test
            void teste1() {
                Reserva reserva = criarReserva(5L, 7L, 3L, INICIO, FIM);
                when(repository.findById(5L)).thenReturn(Optional.of(reserva));

                ReservaDto dto = service.buscarReservaPorId(5L);

                assertEquals(dto.id(), reserva.getId());
                assertEquals(dto.salaId(), reserva.getSalaId());
                assertEquals(dto.usuarioId(), reserva.getUsuarioId());
                assertEquals(dto.dataInicio(), reserva.getDataInicio());
                assertEquals(dto.dataFim(), reserva.getDataFim());
                assertEquals(dto.statusReserva(), reserva.getStatusReserva());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado id inexistente")
            @Test
            void teste1() {
                when(repository.findById(5L)).thenReturn(Optional.empty());

                assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarReservaPorId(5L));
            }
        }
    }

    @DisplayName("Quando listar reservas")
    @Nested
    class ListarReservas {
        private final Pageable pageable = PageRequest.of(0, 20);

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado reservas existentes, listar todas")
            @Test
            void teste1() {
                Reserva primeira = criarReserva(1L, 7L, 3L, INICIO, FIM);
                Reserva segunda = criarReserva(2L, 8L, 4L, INICIO.plusDays(1), FIM.plusDays(1));
                when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(primeira, segunda), pageable, 2));

                Page<ReservaDto> pagina = service.listarTodasReservas(pageable);

                assertEquals(2, pagina.getTotalElements());
            }

            @DisplayName("Dado reservas existentes, listar ativas")
            @Test
            void teste2() {
                Reserva ativa = criarReserva(1L, 7L, 3L, INICIO, FIM);
                when(repository.findByStatusReserva(StatusReserva.ATIVA, pageable))
                        .thenReturn(new PageImpl<>(List.of(ativa), pageable, 1));

                Page<ReservaDto> pagina = service.listarReservasAtivas(pageable);

                assertEquals(1, pagina.getTotalElements());
            }

            @DisplayName("Dado um periodo válido")
            @Test
            void teste3() {
                Reserva reserva = criarReserva(1L, 7L, 3L, INICIO, FIM);
                when(repository.buscarPorPeriodo(INICIO, FIM, pageable))
                        .thenReturn(new PageImpl<>(List.of(reserva), pageable, 1));

                Page<ReservaDto> pagina = service.buscarReservasPorPeriodo(INICIO, FIM, pageable);

                assertEquals(1, pagina.getTotalElements());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado um periodo invalido")
            @Test
            void teste1() {
                assertThrows(ValidacaoException.class, () -> service.buscarReservasPorPeriodo(FIM, INICIO, pageable));

                verifyNoInteractions(repository);
            }
        }
    }

    @DisplayName("Quando listar salas disponiveis")
    @Nested
    class ListarSalasDisponiveis {
        private final Pageable pageable = PageRequest.of(0, 20);

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado uma sala ocupada no periodo, ela não é listada")
            @Test
            void teste1() {
                when(salaClient.listarSalasAtivas())
                        .thenReturn(List.of(criarSalaDto(1L, true), criarSalaDto(2L, true), criarSalaDto(3L, true)));
                when(repository.buscarIdsSalasOcupadas(INICIO, FIM, StatusReserva.ATIVA)).thenReturn(List.of(2L));

                Page<SalaDto> pagina = service.listarSalasDisponiveis(INICIO, FIM, pageable);

                assertEquals(2, pagina.getTotalElements());
            }

            @DisplayName("Dado nenhuma sala ocupada, deve listar todas")
            @Test
            void teste2() {
                when(salaClient.listarSalasAtivas())
                        .thenReturn(List.of(criarSalaDto(1L, true), criarSalaDto(2L, true), criarSalaDto(3L, true)));
                when(repository.buscarIdsSalasOcupadas(INICIO, FIM, StatusReserva.ATIVA)).thenReturn(List.of());

                Page<SalaDto> pagina = service.listarSalasDisponiveis(INICIO, FIM, pageable);

                assertEquals(3, pagina.getTotalElements());
            }

            @DisplayName("Dado todas salas ocupadas, deve listar nenhuma")
            @Test
            void teste3() {
                when(salaClient.listarSalasAtivas())
                        .thenReturn(List.of(criarSalaDto(1L, true), criarSalaDto(2L, true), criarSalaDto(3L, true)));
                when(repository.buscarIdsSalasOcupadas(INICIO, FIM, StatusReserva.ATIVA)).thenReturn(List.of(1L, 2L, 3L));

                Page<SalaDto> pagina = service.listarSalasDisponiveis(INICIO, FIM, pageable);

                assertEquals(0, pagina.getTotalElements());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado um periodo inválido")
            @Test
            void teste1() {
                assertThrows(ValidacaoException.class, () -> service.listarSalasDisponiveis(FIM, INICIO, pageable));

                verifyNoInteractions(salaClient, repository);
            }

            @DisplayName("Dado salas-ms fora do ar")
            @Test
            void teste2() {
                when(salaClient.listarSalasAtivas()).thenThrow(mock(FeignException.class));

                assertThrows(FeignException.class, () -> service.listarSalasDisponiveis(INICIO, FIM, pageable));

                verifyNoInteractions(repository);
            }
        }
    }
}