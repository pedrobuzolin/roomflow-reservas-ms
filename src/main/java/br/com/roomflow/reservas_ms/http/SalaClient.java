package br.com.roomflow.reservas_ms.http;

import br.com.roomflow.reservas_ms.dto.SalaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "salas-ms", url = "${clients.salas.url}")
public interface SalaClient {
    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/salas/{id}")
    SalaDto verificaSala(@PathVariable Long id);

    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/salas")
    List<SalaDto> listarSalasAtivas();
}