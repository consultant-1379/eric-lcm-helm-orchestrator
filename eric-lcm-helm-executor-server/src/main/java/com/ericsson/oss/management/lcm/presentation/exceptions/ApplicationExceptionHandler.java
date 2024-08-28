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

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ericsson.oss.management.lcm.api.model.ProblemDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@Order(HIGHEST_PRECEDENCE)
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_MESSAGE = "{} Occurred, {}";

    @ExceptionHandler(InvalidInputException.class)
    public final ResponseEntity<ProblemDetails> handleInvalidInputException(final InvalidInputException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Invalid Input Exception");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(InvalidFileException.class)
    public final ResponseEntity<ProblemDetails> handleInvalidFileException(final InvalidFileException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Invalid File Exception");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public final ResponseEntity<ProblemDetails> handleResourceNotFoundException(final ResourceNotFoundException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Not Found Exception");
        problemDetails.setType(NOT_FOUND.getReasonPhrase());
        problemDetails.setStatus(NOT_FOUND.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, NOT_FOUND);
    }

    @ExceptionHandler(NotInstantiatedException.class)
    public final ResponseEntity<ProblemDetails> handleNotInstantiatedException(final NotInstantiatedException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("This resource is not in the INSTANTIATED state.");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @ExceptionHandler(ClusterConnectionInfoInUseException.class)
    public final ResponseEntity<ProblemDetails> handleClusterConnectionInfoInUseException(final ClusterConnectionInfoInUseException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Cluster IN_USE exception");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @ExceptionHandler(NotUniqueClusterException.class)
    public final ResponseEntity<ProblemDetails> handleNotUniqueClusterException(final NotUniqueClusterException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("ClusterConnectionInfoInUseException");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @ExceptionHandler(NotUniqueWorkloadInstanceException.class)
    public final ResponseEntity<ProblemDetails> handleNotUniqueWorkloadInstanceException(NotUniqueWorkloadInstanceException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Not Unique WorkloadInstance Exception");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @ExceptionHandler(InternalRuntimeException.class)
    public final ResponseEntity<ProblemDetails> handleInternalRuntimeException(final InternalRuntimeException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("InternalRuntimeException");
        problemDetails.setType(INTERNAL_SERVER_ERROR.getReasonPhrase());
        problemDetails.setStatus(INTERNAL_SERVER_ERROR.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FileServiceException.class)
    public final ResponseEntity<ProblemDetails> handleFileServiceException(final FileServiceException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("FileServiceException");
        problemDetails.setType(INTERNAL_SERVER_ERROR.getReasonPhrase());
        problemDetails.setStatus(INTERNAL_SERVER_ERROR.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InstanceNotTerminatedException.class)
    public final ResponseEntity<ProblemDetails> handleNotTerminatedInstance(final InstanceNotTerminatedException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Conflict Exception");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            MissingServletRequestPartException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ERROR_MESSAGE, ex.toString(), ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Malformed Request");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPaginationQueryException.class)
    public final ResponseEntity<ProblemDetails> handleInvalidPaginationQueryException(final InvalidPaginationQueryException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("InvalidPaginationQueryException");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(NotValidClusterNameException.class)
    public final ResponseEntity<ProblemDetails> handleNotValidClusterNameException(final NotValidClusterNameException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("NotValidClusterNameException");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(ClusterConnectionInfoConnectionException.class)
    public final ResponseEntity<ProblemDetails> handleClusterConnectionInfoConnectionException(final ClusterConnectionInfoConnectionException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("ClusterConnectionInfoConnectionException");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(DockerRegistrySecretException.class)
    public final ResponseEntity<ProblemDetails> handleDockerRegistrySecretException(final DockerRegistrySecretException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("DockerRegistrySecretException");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(NotUniqueHelmSourceException.class)
    public final ResponseEntity<ProblemDetails> handleNotUniqueHelmSourceException(final NotUniqueHelmSourceException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("NotUniqueHelmSourceException");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public final ResponseEntity<ProblemDetails> handleUnsupportedOperation(final UnsupportedOperationException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("UnsupportedOperationException");
        problemDetails.setType(NOT_IMPLEMENTED.getReasonPhrase());
        problemDetails.setStatus(NOT_IMPLEMENTED.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, NOT_IMPLEMENTED);
    }

    @ExceptionHandler(IncorrectRollbackRequestException.class)
    public final ResponseEntity<ProblemDetails> handleIncorrectRollbackRequestException(final IncorrectRollbackRequestException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("IncorrectRollbackRequestException");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(HelmRepoException.class)
    public final ResponseEntity<ProblemDetails> handleHelmRepoException(final HelmRepoException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("HelmRepoException");
        problemDetails.setType(INTERNAL_SERVER_ERROR.getReasonPhrase());
        problemDetails.setStatus(INTERNAL_SERVER_ERROR.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BackupAndRestoreConnectionException.class)
    public final ResponseEntity<ProblemDetails> handleBackupAndRestoreConnectionException(final BackupAndRestoreConnectionException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("BackupAndRestoreConnectionException");
        problemDetails.setType(SERVICE_UNAVAILABLE.getReasonPhrase());
        problemDetails.setStatus(SERVICE_UNAVAILABLE.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(BackupAndRestoreHttpClientException.class)
    public final ResponseEntity<ProblemDetails> handleBackupAndRestoreHttpClientException(final BackupAndRestoreHttpClientException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("BackupAndRestoreHttpClientException");
        problemDetails.setType(SERVICE_UNAVAILABLE.getReasonPhrase());
        problemDetails.setStatus(SERVICE_UNAVAILABLE.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(BackupAndRestoreException.class)
    public final ResponseEntity<ProblemDetails> handleBackupAndRestoreException(final BackupAndRestoreException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("BackupAndRestoreException");
        problemDetails.setType(CONFLICT.getReasonPhrase());
        problemDetails.setStatus(CONFLICT.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, CONFLICT);
    }

    @ExceptionHandler(SecurityOperationException.class)
    public final ResponseEntity<ProblemDetails> handleCryptoException(final SecurityOperationException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("SecurityOperationException");
        problemDetails.setType(BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(BAD_REQUEST.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    @ExceptionHandler(GitRepoConnectionException.class)
    public ResponseEntity<ProblemDetails> handleGitRepoConnectionException(GitRepoConnectionException ex) {
        log.error(ERROR_MESSAGE, ex, ex.getMessage());
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("GitRepoConnectionException");
        problemDetails.setType(SERVICE_UNAVAILABLE.getReasonPhrase());
        problemDetails.setStatus(SERVICE_UNAVAILABLE.value());
        problemDetails.setInstance("");
        problemDetails.setDetail(ex.getMessage());
        return new ResponseEntity<>(problemDetails, SERVICE_UNAVAILABLE);
    }
}
