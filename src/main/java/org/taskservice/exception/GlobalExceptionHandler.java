package org.taskservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps database-outage failures to a clean 503 instead of a bare 500.
 * Spring splits these into two unrelated exception hierarchies:
 * DataAccessException (a query itself failing) and TransactionException
 * (unable to even open a transaction/connection, which is what a database
 * being down actually throws -- CannotCreateTransactionException). Both
 * need to be caught here; neither is a subtype of the other.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(RuntimeException ex) {
        log.error("Database access failed: {}", ex.toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Service temporarily unavailable, please try again shortly"));
    }
}
