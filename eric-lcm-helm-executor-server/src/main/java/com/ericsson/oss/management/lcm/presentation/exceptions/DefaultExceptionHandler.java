/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.management.lcm.presentation.exceptions;

import com.ericsson.oss.management.lcm.api.model.ProblemDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@Slf4j
@ControllerAdvice
@Order(LOWEST_PRECEDENCE)
public class DefaultExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleAll(Throwable throwable) {
        String simpleName = throwable.getClass().getSimpleName();
        log.error("Error occurred {} with error {}", simpleName, throwable);
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Error occurred " + simpleName);
        problemDetails.setType(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        problemDetails.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(throwable.getMessage());
        return new ResponseEntity<>(problemDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
