package br.sentineltrade.bff.comum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Normaliza TODOS os erros para { "message": "texto para o usuario" } com o status certo.
 * Nunca devolve stack trace nem detalhes internos.
 */
@RestControllerAdvice
public class ErroHandler {

    private static final Logger log = LoggerFactory.getLogger(ErroHandler.class);

    @ExceptionHandler(BffException.class)
    public ResponseEntity<Map<String, String>> bff(BffException e) {
        return erro(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(MethodArgumentNotValidException e) {
        FieldError campo = e.getBindingResult().getFieldError();
        String mensagem = campo != null && campo.getDefaultMessage() != null
                ? campo.getDefaultMessage()
                : "Dados inválidos.";
        return erro(400, mensagem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(HandlerMethodValidationException e) {
        return erro(400, "Dados inválidos.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(HttpMessageNotReadableException e) {
        return erro(400, "Dados inválidos ou mal formatados.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<Map<String, String>> faltando(Exception e) {
        return erro(400, "Dados obrigatórios ausentes.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> tipoNaoSuportado(HttpMediaTypeNotSupportedException e) {
        return erro(415, "Envie os dados no formato JSON.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> metodo(HttpRequestMethodNotSupportedException e) {
        return erro(405, "Método não permitido.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(NoResourceFoundException e) {
        return erro(404, "Recurso não encontrado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception e) {
        log.error("Erro inesperado no BFF", e);
        return erro(500, "Erro inesperado. Tente novamente.");
    }

    private static ResponseEntity<Map<String, String>> erro(int status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("message", mensagem));
    }
}
