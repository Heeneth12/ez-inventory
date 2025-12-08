package com.ezh.Inventory.utils.exception;

import com.ezh.Inventory.utils.common.ResponseResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResponseResource<?>> handleCommonException(CommonException ex) {
        log.error("Service Exception occurred: {}", ex.getMessage());
        return new ResponseEntity<>(
                ResponseResource.error(ex.getHttpStatus(), ex.getMessage()),
                ex.getHttpStatus()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResource<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("Validation failed: {}", errorMessage);
        return new ResponseEntity<>(
                ResponseResource.error(HttpStatus.BAD_REQUEST, errorMessage),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResource<?>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred {}", ex.getMessage());
        return new ResponseEntity<>(
                ResponseResource.error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
