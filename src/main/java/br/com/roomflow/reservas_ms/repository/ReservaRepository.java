package br.com.roomflow.reservas_ms.repository;

import br.com.roomflow.reservas_ms.model.Reserva;
import br.com.roomflow.reservas_ms.model.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

        Page<Reserva> findByStatusReserva(StatusReserva statusReserva, Pageable pageable);

        @Query(value = """
            SELECT r FROM Reserva r
            WHERE r.dataInicio < :fim
            AND r.dataFim > :inicio
            """,
                countQuery = """
            SELECT COUNT(r) FROM Reserva r
            WHERE r.dataInicio < :fim
            AND r.dataFim > :inicio
            """)
        Page<Reserva> buscarPorPeriodo(
                @Param("inicio") LocalDateTime inicio,
                @Param("fim") LocalDateTime fim,
                Pageable pageable);

        @Query("""
        SELECT DISTINCT r.salaId FROM Reserva r
        WHERE r.statusReserva = :status
        AND r.dataInicio < :fim
        AND r.dataFim > :inicio
        """)
        List<Long> buscarIdsSalasOcupadas(
                @Param("inicio") LocalDateTime inicio,
                @Param("fim") LocalDateTime fim,
                @Param("status") StatusReserva status);
}
