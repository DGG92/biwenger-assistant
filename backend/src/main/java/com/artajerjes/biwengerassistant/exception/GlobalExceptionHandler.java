package com.artajerjes.biwengerassistant.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.artajerjes.biwengerassistant.league.LeagueAlreadyExistsException;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.manager.ManagerNotFoundException;
import com.artajerjes.biwengerassistant.player.PlayerAlreadyExistsException;
import com.artajerjes.biwengerassistant.player.PlayerNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {
                Map<String, String> fields = new LinkedHashMap<>();

                exception.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));

                ApiError apiError = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation failed",
                                "One or more fields are invalid",
                                request.getRequestURI(),
                                fields);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(apiError);
        }

        @ExceptionHandler(LeagueAlreadyExistsException.class)
        public ResponseEntity<ApiError> handleLeagueAlreadyExists(
                        LeagueAlreadyExistsException exception,
                        HttpServletRequest request) {
                ApiError apiError = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(apiError);
        }

        @ExceptionHandler(LeagueNotFoundException.class)
        public ResponseEntity<ApiError> handleLeagueNotFound(
                        LeagueNotFoundException exception,
                        HttpServletRequest request) {
                ApiError apiError = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(apiError);
        }

        @ExceptionHandler(PlayerNotFoundException.class)
        public ResponseEntity<ApiError> handlePlayerNotFound(
                        PlayerNotFoundException exception,
                        HttpServletRequest request) {
                ApiError apiError = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(apiError);

        }

        @ExceptionHandler(PlayerAlreadyExistsException.class)
        public ResponseEntity<ApiError> handlePlayerAlreadyExists(
                        PlayerAlreadyExistsException exception,
                        HttpServletRequest request) {
                ApiError apiError = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(apiError);
        }

        @ExceptionHandler(ManagerNotFoundException.class)
        public ResponseEntity<ApiError> handleManagerNotFound(
                        ManagerNotFoundException exception,
                        HttpServletRequest request) {
                ApiError apiError = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(apiError);
        }

}