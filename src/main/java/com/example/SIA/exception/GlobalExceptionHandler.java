package com.example.SIA.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------
    // ERRORES DE VISTA
    // ---------------------------
    @ExceptionHandler(BadGatewayException.class)
    public String handleBadGateway(Model model, BadGatewayException ex) {
        model.addAttribute("mensaje",
                ex.getMessage() != null ? ex.getMessage() : "Error de comunicación con el servidor externo.");
        return "error/502";
    }

    // ---------------------------
    // ARCHIVO MUY GRANDE
    // ---------------------------
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        logger.warn("Archivo demasiado grande", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "El archivo es demasiado grande. Máximo permitido: 20MB.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // ---------------------------
    // RECURSOS ESTÁTICOS (favicon, css, js)
    // ---------------------------
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    // ---------------------------
    // ERROR GLOBAL REAL
    // ---------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        logger.error("Error interno no controlado", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Error interno del servidor");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
