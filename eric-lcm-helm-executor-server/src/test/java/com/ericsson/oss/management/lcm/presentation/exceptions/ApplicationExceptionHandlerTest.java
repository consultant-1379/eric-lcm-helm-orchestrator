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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import com.ericsson.oss.management.lcm.api.model.ProblemDetails;

class ApplicationExceptionHandlerTest {

    private static final String INTERNAL_RUNTIME_EXCEPTION = "InternalRuntimeException";
    private static final String FILE_SERVICE_EXCEPTION = "FileServiceException";
    private static final String INVALID_PAGINATION_QUERY_EXCEPTION = "InvalidPaginationQueryException";
    private static final String INVALID_INPUT_EXCEPTION = "Invalid Input Exception";
    private static final String NOT_VALID_CLUSTER_NAME_EXCEPTION = "NotValidClusterNameException";
    private static final String BACKUP_AND_RESTORE_CONNECTION_EXCEPTION = "BackupAndRestoreConnectionException";
    private static final String BACKUP_AND_RESTORE_HTTP_CLIENT_EXCEPTION = "BackupAndRestoreHttpClientException";
    private static final String BACKUP_AND_RESTORE_EXCEPTION = "BackupAndRestoreException";
    private static final String SECURITY_OPERATION_EXCEPTION = "SecurityOperationException";
    private static final String GIT_REPO_CONNECTION_EXCEPTION = "GitRepoConnectionException";

    ApplicationExceptionHandler applicationExceptionHandler = new ApplicationExceptionHandler();
    public static final String ERROR = "This is a test";

    @Test
    void handleInternalRuntimeExceptionTest() {
        try {
            throw new InternalRuntimeException(ERROR);
        } catch (InternalRuntimeException e) {
            ResponseEntity<ProblemDetails> responseEntity = applicationExceptionHandler.handleInternalRuntimeException(
                    e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(INTERNAL_RUNTIME_EXCEPTION);
            assertThat(result.getType()).isEqualTo(INTERNAL_SERVER_ERROR.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    void handleFileServiceExceptionTest() {
        try {
            throw new FileServiceException(ERROR, new RuntimeException());
        } catch (FileServiceException e) {
            ResponseEntity<ProblemDetails> responseEntity = applicationExceptionHandler.handleFileServiceException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(FILE_SERVICE_EXCEPTION);
            assertThat(result.getType()).isEqualTo(INTERNAL_SERVER_ERROR.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    void handleInvalidPaginationQueryExceptionTest() {
        try {
            throw new com.ericsson.oss.management.lcm.presentation.exceptions.InvalidPaginationQueryException(ERROR);
        } catch (InvalidPaginationQueryException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleInvalidPaginationQueryException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(INVALID_PAGINATION_QUERY_EXCEPTION);
            assertThat(result.getType()).isEqualTo(BAD_REQUEST.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(BAD_REQUEST.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    void handleInvalidInputExceptionTest() {
        try {
            throw new com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException(ERROR);
        } catch (InvalidInputException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleInvalidInputException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(INVALID_INPUT_EXCEPTION);
            assertThat(result.getType()).isEqualTo(BAD_REQUEST.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(BAD_REQUEST.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    void handleNotValidClusterNameExceptionTest() {
        try {
            throw new NotValidClusterNameException(ERROR);
        } catch (NotValidClusterNameException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleNotValidClusterNameException(
                    e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(NOT_VALID_CLUSTER_NAME_EXCEPTION);
            assertThat(result.getType()).isEqualTo(BAD_REQUEST.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(BAD_REQUEST.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    void handleClusterConnectionInfoConnectionExceptionTest() {
        try {
            throw new ClusterConnectionInfoConnectionException(ERROR);
        } catch (ClusterConnectionInfoConnectionException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleClusterConnectionInfoConnectionException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(("ClusterConnectionInfoConnectionException"));
            assertThat(result.getType()).isEqualTo(BAD_REQUEST.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(BAD_REQUEST.value());
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    void handleClusterConnectionInfoInUseExceptionTest() {
        try {
            throw new ClusterConnectionInfoInUseException(ERROR);
        } catch (ClusterConnectionInfoInUseException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleClusterConnectionInfoInUseException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(("Cluster IN_USE exception"));
            assertThat(result.getType()).isEqualTo(CONFLICT.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(CONFLICT.value());
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(CONFLICT);
        }
    }

    @Test
    void handleUnsupportedOperationExceptionTest() {
        try {
            throw new UnsupportedOperationException(ERROR);
        } catch (UnsupportedOperationException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleUnsupportedOperation(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(("UnsupportedOperationException"));
            assertThat(result.getType()).isEqualTo(NOT_IMPLEMENTED.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(NOT_IMPLEMENTED.value());
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(NOT_IMPLEMENTED);
        }
    }

    @Test
    void handleHelmRepoExceptionTest() {
        try {
            throw new HelmRepoException(ERROR);
        } catch (HelmRepoException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleHelmRepoException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(("HelmRepoException"));
            assertThat(result.getType()).isEqualTo(INTERNAL_SERVER_ERROR.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    void handleBackupAndRestoreConnectionExceptionTest() {
        try {
            throw new BackupAndRestoreConnectionException(ERROR);
        } catch (BackupAndRestoreConnectionException e) {
            ResponseEntity<ProblemDetails> responseEntity = applicationExceptionHandler.handleBackupAndRestoreConnectionException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(BACKUP_AND_RESTORE_CONNECTION_EXCEPTION);
            assertThat(result.getType()).isEqualTo(SERVICE_UNAVAILABLE.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(SERVICE_UNAVAILABLE.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(SERVICE_UNAVAILABLE);
        }
    }

    @Test
    void handleBackupAndRestoreHttpClientExceptionTest() {
        try {
            throw new BackupAndRestoreHttpClientException(ERROR);
        } catch (BackupAndRestoreHttpClientException e) {
            ResponseEntity<ProblemDetails> responseEntity = applicationExceptionHandler.handleBackupAndRestoreHttpClientException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(BACKUP_AND_RESTORE_HTTP_CLIENT_EXCEPTION);
            assertThat(result.getType()).isEqualTo(SERVICE_UNAVAILABLE.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(SERVICE_UNAVAILABLE.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(SERVICE_UNAVAILABLE);
        }
    }

    @Test
    void handleBackupAndRestoreExceptionTest() {
        try {
            throw new BackupAndRestoreException(ERROR);
        } catch (BackupAndRestoreException e) {
            ResponseEntity<ProblemDetails> responseEntity = applicationExceptionHandler.handleBackupAndRestoreException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(BACKUP_AND_RESTORE_EXCEPTION);
            assertThat(result.getType()).isEqualTo(CONFLICT.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(CONFLICT.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(CONFLICT);
        }
    }

    @Test
    void handleSecurityOperationExceptionTest() {
        try {
            throw new SecurityOperationException(ERROR);
        } catch (SecurityOperationException e) {
            ResponseEntity<ProblemDetails> responseEntity = applicationExceptionHandler.handleCryptoException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result.getTitle()).isEqualTo(SECURITY_OPERATION_EXCEPTION);
            assertThat(result.getType()).isEqualTo(BAD_REQUEST.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(BAD_REQUEST.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    void handleGitRepoConnectionExceptionTest() {
        try {
            throw new GitRepoConnectionException(ERROR);
        } catch (GitRepoConnectionException e) {
            ResponseEntity<ProblemDetails> responseEntity =
                    applicationExceptionHandler.handleGitRepoConnectionException(e);
            ProblemDetails result = responseEntity.getBody();
            HttpStatusCode resultCode = responseEntity.getStatusCode();

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(GIT_REPO_CONNECTION_EXCEPTION);
            assertThat(result.getType()).isEqualTo(SERVICE_UNAVAILABLE.getReasonPhrase());
            assertThat(result.getStatus()).isEqualTo(SERVICE_UNAVAILABLE.value());
            assertThat(result.getInstance()).isEmpty();
            assertThat(result.getDetail()).isEqualTo(ERROR);

            assertThat(resultCode).isEqualTo(SERVICE_UNAVAILABLE);
        }
    }
}
