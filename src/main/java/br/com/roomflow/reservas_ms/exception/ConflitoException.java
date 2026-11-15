package br.com.roomflow.reservas_ms.exception;

import static br.com.roomflow.reservas_ms.exception.TipoErro.CONFLITO;

public class ConflitoException extends ApiException {
    public ConflitoException(String message) {
        super(message, CONFLITO);
    }
}
