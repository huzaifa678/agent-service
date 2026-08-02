package com.project.agent.adapter.in.rest;

import com.project.agent.adapter.in.rest.dto.ErrorResponse;
import com.project.agent.application.shared.exception.ApplicationException;
import com.project.agent.application.shared.exception.ResourceNotFoundException;
import com.project.agent.domain.exception.AggregateNotFoundException;
import com.project.agent.domain.exception.BusinessRuleViolationException;
import com.project.agent.domain.exception.DomainException;
import com.project.agent.domain.execution.agent.exception.AgentExecutionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and application exceptions to HTTP responses. Handler
 * specificity resolves the hierarchy: a {@code ConversationNotFoundException}
 * (→ {@code AggregateNotFoundException}) maps to 404, a
 * {@code BusinessRuleViolationException} to 422, upstream model failures to 502,
 * and any remaining {@code DomainException} to 502.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, AggregateNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException e) {
        return ErrorResponse.of(e.getClass().getSimpleName(), e.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleBusinessRule(BusinessRuleViolationException e) {
        return ErrorResponse.of(e.getClass().getSimpleName(), e.getMessage());
    }

    @ExceptionHandler(AgentExecutionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleAgentFailure(AgentExecutionFailedException e) {
        log.warn("Agent execution failed", e);
        return ErrorResponse.of(e.getClass().getSimpleName(), e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleDomain(DomainException e) {
        log.warn("Domain/infrastructure failure", e);
        return ErrorResponse.of(e.getClass().getSimpleName(), e.getMessage());
    }

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleApplication(ApplicationException e) {
        return ErrorResponse.of(e.getClass().getSimpleName(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(Exception e) {
        return ErrorResponse.of("ValidationError", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ErrorResponse.of("InternalError", "An unexpected error occurred.");
    }
}
