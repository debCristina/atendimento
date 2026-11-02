package com.sea.atendimento.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    // Dados inválidos no corpo (@RequestBody) ou nos parâmetros da URL
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleValidation(BindException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldErrorDetail.of(error.getField(),
                        error.isBindingFailure() ? "Valor inválido" : error.getDefaultMessage()))
                .toList();

        return respond(HttpStatus.BAD_REQUEST, "Dados inválidos", request, fieldErrors);
    }

    // JSON mal formatado ou com tipos incompatíveis
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", request);
    }

    // ===== 401 =====

    // E-mail ou senha incorretos no login
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex,
                                                             HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // Sem token, token inválido ou expirado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "Autenticação necessária: envie um token válido", request);
    }

    // ===== 403 =====

    // Autenticado, mas sem permissão para o recurso
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso", request);
    }

    // ===== 404 =====

    // Recurso inexistente ou sem acesso (dono ou UF)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ===== 409 =====

    // Conflito com o estado atual (e-mail já cadastrado, status que não permite a ação)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // O banco recusou por violar uma constraint (ex.: UNIQUE sob concorrência)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "A operação conflita com dados já existentes", request);
    }

    // ===== 422 =====

    // Regra de negócio não atendida (com a lista de campos e etapas, quando houver)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex,
                                                       HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, ex.getFieldErrors());
    }

    // ===== 503 =====

    // Serviço externo (como o ViaCEP) indisponível
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternalService(ExternalServiceException ex,
                                                          HttpServletRequest request) {
        log.warn("Falha em serviço externo em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    // ===== Rede de segurança =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Exceções do próprio Spring que já sabem seu status (ex.: 404 de rota, 405)
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return respond(status, errorResponse.getBody().getDetail(), request);
        }

        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    // ===== Auxiliares =====

    private ResponseEntity<ApiError> respond(HttpStatus status, String message,
                                             HttpServletRequest request,
                                             List<FieldErrorDetail> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, message, request.getRequestURI(), fieldErrors));
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message,
                                             HttpServletRequest request) {
        return respond(status, message, request, List.of());
    }
}