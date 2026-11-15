package br.com.roomflow.reservas_ms.controller;

import br.com.roomflow.reservas_ms.dto.ReservaDto;
import br.com.roomflow.reservas_ms.dto.SalaDto;
import br.com.roomflow.reservas_ms.dto.SolicitacaoReservaDto;
import br.com.roomflow.reservas_ms.exception.ConflitoException;
import br.com.roomflow.reservas_ms.exception.RecursoNaoEncontradoException;
import br.com.roomflow.reservas_ms.exception.ValidacaoException;
import br.com.roomflow.reservas_ms.model.StatusReserva;
import br.com.roomflow.reservas_ms.service.ReservaService;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(ReservaController.class)
class ReservaControllerTest {
    private static final LocalDateTime INICIO = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime FIM = LocalDateTime.now().plusHours(2);

    @MockitoBean
    private ReservaService reservaService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private FeignException servicoIndisponivel() {
        return new FeignException(503, "Serviço indisponível") {
        };
    }

    @DisplayName("Quando listar as reservas")
    @Nested
    class ListarReservas {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {

            @DisplayName("Ao listar todas as reservas")
            @Test
            void teste1() throws Exception {
                when(reservaService.listarTodasReservas(any(Pageable.class))).thenReturn(Page.empty());

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas")
                ).andReturn().getResponse();

                assertEquals(200, response.getStatus());
            }

            @DisplayName("Ao listar as reservas ativas")
            @Test
            void teste2() throws Exception {
                when(reservaService.listarReservasAtivas(any(Pageable.class))).thenReturn(Page.empty());

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/ativas")
                ).andReturn().getResponse();

                assertEquals(200, response.getStatus());
            }

            @DisplayName("Ao buscar reserva por ID existente")
            @Test
            void teste3() throws Exception {
                ReservaDto reservaDto = new ReservaDto(1L, 7L, 3L, INICIO, FIM, StatusReserva.ATIVA);
                when(reservaService.buscarReservaPorId(1L)).thenReturn(reservaDto);

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/{id}", 1)
                ).andReturn().getResponse();

                assertEquals(200, response.getStatus());
            }

            @DisplayName("Ao buscar reservas por período válido")
            @Test
            void teste4() throws Exception {
                when(reservaService.buscarReservasPorPeriodo(eq(INICIO), eq(FIM), any(Pageable.class)))
                        .thenReturn(Page.empty());

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/periodo")
                                .param("dataInicio", INICIO.toString())
                                .param("dataFim", FIM.toString())
                ).andReturn().getResponse();

                assertEquals(200, response.getStatus());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {

            @DisplayName("Ao buscar reserva com ID inexistente")
            @Test
            void teste1() throws Exception {
                Long id = 1L;
                when(reservaService.buscarReservaPorId(id)).thenThrow(new RecursoNaoEncontradoException("Reserva não encontrada!"));

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/{id}", id)
                ).andReturn().getResponse();

                assertEquals(404, response.getStatus());
            }

            @DisplayName("Ao buscar reserva com ID que não é número")
            @Test
            void teste2() throws Exception {
                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/{id}", "abc")
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Ao buscar por período sem informar a data final")
            @Test
            void teste3() throws Exception {
                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/periodo")
                                .param("dataInicio", INICIO.toString())
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Ao buscar por período com data em formato inválido")
            @Test
            void teste4() throws Exception {
                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/periodo")
                                .param("dataInicio", "abc")
                                .param("dataFim", FIM.toString())
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Ao buscar por período com data inicial depois da final")
            @Test
            void teste5() throws Exception {
                when(reservaService.buscarReservasPorPeriodo(eq(FIM), eq(INICIO), any(Pageable.class)))
                        .thenThrow(new ValidacaoException("A data inicial deve ser anterior à data final!"));

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/periodo")
                                .param("dataInicio", FIM.toString())
                                .param("dataFim", INICIO.toString())
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }
        }
    }

    @DisplayName("Quando listar as salas disponíveis")
    @Nested
    class ListarSalasDisponiveis {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {

            @DisplayName("Ao informar um período válido")
            @Test
            void teste1() throws Exception {
                Page<SalaDto> salas = new PageImpl<>(List.of(new SalaDto(1L, "Sala Azul", 20, true)));
                when(reservaService.listarSalasDisponiveis(eq(INICIO), eq(FIM), any(Pageable.class)))
                        .thenReturn(salas);

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/salas-disponiveis")
                                .param("dataInicio", INICIO.toString())
                                .param("dataFim", FIM.toString())
                ).andReturn().getResponse();

                assertEquals(200, response.getStatus());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {

            @DisplayName("Ao informar data inicial depois da final")
            @Test
            void teste1() throws Exception {
                when(reservaService.listarSalasDisponiveis(eq(FIM), eq(INICIO), any(Pageable.class)))
                        .thenThrow(new ValidacaoException("A data inicial deve ser anterior à data final!"));

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/salas-disponiveis")
                                .param("dataInicio", FIM.toString())
                                .param("dataFim", INICIO.toString())
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Ao não informar o período")
            @Test
            void teste2() throws Exception {
                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/salas-disponiveis")
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Dado salas-ms fora do ar")
            @Test
            void teste3() throws Exception {
                when(reservaService.listarSalasDisponiveis(eq(INICIO), eq(FIM), any(Pageable.class)))
                        .thenThrow(servicoIndisponivel());

                MockHttpServletResponse response = mockMvc.perform(
                        get("/api/v1/reservas/salas-disponiveis")
                                .param("dataInicio", INICIO.toString())
                                .param("dataFim", FIM.toString())
                ).andReturn().getResponse();

                assertEquals(503, response.getStatus());
            }
        }
    }

    @DisplayName("Quando criar uma reserva")
    @Nested
    class CriarReserva {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {

            @DisplayName("Dado uma reserva com todos os campos")
            @Test
            void teste1() throws Exception {
                SolicitacaoReservaDto dto = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(reservaService.criarReserva(dto)).thenReturn(1L);

                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                ).andReturn().getResponse();

                assertEquals(201, response.getStatus());
                assertEquals("http://localhost/api/v1/reservas/1", response.getHeader("Location"));
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {

            @DisplayName("Dado uma reserva com campos nulos")
            @Test
            void teste1() throws Exception {
                SolicitacaoReservaDto dtoInvalido = new SolicitacaoReservaDto(null, null, null, null);

                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dtoInvalido))
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Dado um corpo que não é um JSON válido")
            @Test
            void teste2() throws Exception {
                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{")
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Dado um horário já reservado para a sala")
            @Test
            void teste3() throws Exception {
                SolicitacaoReservaDto dto = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(reservaService.criarReserva(dto))
                        .thenThrow(new ConflitoException("A sala já está reservada neste período!"));

                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                ).andReturn().getResponse();

                assertEquals(409, response.getStatus());
            }

            @DisplayName("Dado uma sala inativa")
            @Test
            void teste4() throws Exception {
                SolicitacaoReservaDto dto = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(reservaService.criarReserva(dto)).thenThrow(new ValidacaoException("Sala inativa!"));

                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                ).andReturn().getResponse();

                assertEquals(400, response.getStatus());
            }

            @DisplayName("Dado uma sala inexistente")
            @Test
            void teste5() throws Exception {
                SolicitacaoReservaDto dto = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(reservaService.criarReserva(dto))
                        .thenThrow(new RecursoNaoEncontradoException("Sala não encontrada!"));

                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                ).andReturn().getResponse();

                assertEquals(404, response.getStatus());
            }

            @DisplayName("Dado que um dos serviços necessários está fora do ar")
            @Test
            void teste6() throws Exception {
                SolicitacaoReservaDto dto = new SolicitacaoReservaDto(7L, 3L, INICIO, FIM);
                when(reservaService.criarReserva(dto)).thenThrow(servicoIndisponivel());

                MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                ).andReturn().getResponse();

                assertEquals(503, response.getStatus());
            }
        }
    }

    @DisplayName("Quando cancelar uma reserva")
    @Nested
    class CancelarReserva {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {

            @DisplayName("Ao cancelar uma reserva com ID válido")
            @Test
            void teste1() throws Exception {
                Long id = 1L;

                MockHttpServletResponse response = mockMvc.perform(
                        patch("/api/v1/reservas/cancelar/{id}", id)
                ).andReturn().getResponse();

                assertEquals(200, response.getStatus());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {

            @DisplayName("Ao tentar cancelar reserva com ID inexistente")
            @Test
            void teste1() throws Exception {
                Long id = 1L;
                doThrow(new RecursoNaoEncontradoException("Reserva não encontrada!"))
                        .when(reservaService).cancelarReserva(id);

                MockHttpServletResponse response = mockMvc.perform(
                        patch("/api/v1/reservas/cancelar/{id}", id)
                ).andReturn().getResponse();

                assertEquals(404, response.getStatus());
            }

            @DisplayName("Ao tentar cancelar uma reserva que já começou")
            @Test
            void teste2() throws Exception {
                Long id = 1L;
                doThrow(new ConflitoException("Não é possível cancelar uma reserva que já começou!"))
                        .when(reservaService).cancelarReserva(id);

                MockHttpServletResponse response = mockMvc.perform(
                        patch("/api/v1/reservas/cancelar/{id}", id)
                ).andReturn().getResponse();

                assertEquals(409, response.getStatus());
            }
        }
    }
}