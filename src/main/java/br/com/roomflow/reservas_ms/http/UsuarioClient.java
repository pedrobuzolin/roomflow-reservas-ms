package br.com.roomflow.reservas_ms.http;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "usuarios-ms", url = "${clients.usuarios.url}")
public interface UsuarioClient {
    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/usuarios/{id}")
    void verificaUsuario(@PathVariable Long id);
}
