package br.com.roomflow.reservas_ms.exception;

import static br.com.roomflow.reservas_ms.exception.TipoErro.VALIDACAO;

public class ValidacaoException extends ApiException {
    public ValidacaoException(String message) {
        super(message, VALIDACAO);
    }
}
