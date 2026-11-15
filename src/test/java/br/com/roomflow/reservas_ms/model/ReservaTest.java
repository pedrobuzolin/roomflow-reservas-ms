package br.com.roomflow.reservas_ms.model;

import br.com.roomflow.reservas_ms.exception.ConflitoException;
import br.com.roomflow.reservas_ms.exception.ValidacaoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class ReservaTest {
    private static final LocalDateTime INICIO = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime FIM = LocalDateTime.now().plusHours(2);

    @DisplayName("Quando criar uma reserva")
    @Nested
    class CriarReserva {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado uma reserva com dados validos")
            @Test
            void teste1() {
               Reserva reserva = new Reserva(7L, 3L, INICIO, FIM);

                assertNull(reserva.getId());
                assertEquals(7L, reserva.getSalaId());
                assertEquals(3L, reserva.getUsuarioId());
                assertEquals(INICIO, reserva.getDataInicio());
                assertEquals(FIM, reserva.getDataFim());
                assertEquals(StatusReserva.ATIVA, reserva.getStatusReserva());
            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado uma sala nula")
            @Test
            void teste1() {
                assertThrows(ValidacaoException.class, () -> new Reserva(null, 3L, INICIO, FIM));
            }

            @DisplayName("Dado um usuario nulo")
            @Test
            void teste2() {
                assertThrows(ValidacaoException.class, () -> new Reserva(7L, null, INICIO, FIM));
            }

            @DisplayName("Dado data de inicio nula")
            @Test
            void teste3() {
                assertThrows(ValidacaoException.class, () -> new Reserva(7L, 3L, null, FIM));
            }

            @DisplayName("Dado data de fim nula")
            @Test
            void teste4() {
                assertThrows(ValidacaoException.class, () -> new Reserva(7L, 3L, INICIO, null));
            }

            @DisplayName("Dado inicio igual ao fim")
            @Test
            void teste5() {
                assertThrows(ValidacaoException.class, () -> new Reserva(7L, 3L, INICIO, INICIO));
            }

            @DisplayName("Dado inicio depois do fim")
            @Test
            void teste6() {
                assertThrows(ValidacaoException.class, () -> new Reserva(7L, 3L, FIM, INICIO));
            }
        }
    }

    @DisplayName("Quando cancelar reserva")
    @Nested
    class CancelarReserva {

        @DisplayName("Então deve executar com sucesso")
        @Nested
        class Sucesso {
            @DisplayName("Dado uma reserva com dados validos")
            @Test
            void teste1() {
                Reserva reserva = new Reserva(7L, 3L, INICIO, FIM);
                reserva.marcarReservaComoCancelada();

                assertEquals(StatusReserva.CANCELADA, reserva.getStatusReserva());

            }
        }

        @DisplayName("Então deve retornar erro")
        @Nested
        class Falha {
            @DisplayName("Dado uma reserva já cancelada")
            @Test
            void teste1() {
                Reserva reserva = new Reserva(7L, 3L, INICIO, FIM);
                setField(reserva, "statusReserva", StatusReserva.CANCELADA);

                assertThrows(ConflitoException.class, reserva::marcarReservaComoCancelada);
                assertEquals(StatusReserva.CANCELADA, reserva.getStatusReserva());
            }

            @DisplayName("Dado uma reserva em andamento")
            @Test
            void teste2() {
                LocalDateTime agora = LocalDateTime.now();
                Reserva reserva = new Reserva(7L, 3L, agora.minusHours(1), agora.plusHours(1));

                assertThrows(ConflitoException.class, reserva::marcarReservaComoCancelada);
                assertEquals(StatusReserva.ATIVA, reserva.getStatusReserva());
            }

            @DisplayName("Dado uma reserva ja encerrada")
            @Test
            void teste3() {
                LocalDateTime agora = LocalDateTime.now();
                Reserva reserva = new Reserva(7L, 3L, agora.minusHours(2), agora.minusHours(1));

                assertThrows(ConflitoException.class, reserva::marcarReservaComoCancelada);
                assertEquals(StatusReserva.ATIVA, reserva.getStatusReserva());
            }
        }
    }
}