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

package com.ericsson.oss.management.lcm.configurations;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.refresh.ContextRefresher;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class SecretContextRefreshEventHandler implements ResourceEventHandler<Secret> {

    private static final String SECRET_ADDED_LOG_MSG = "Secret {} added.";
    private static final String SECRET_UPDATED_LOG_MSG = "Secret {} updated.";
    private static final String SECRET_DATA_UNCHANGED_LOG_MSG = "Secret data unchanged, will not reload.";
    private static final String SECRET_WAS_DELETED_LOG_MSG = "Secret {} was deleted.";
    private static final String START_REFRESH_LOG_MSG = "Start refresh process.";
    private static final String CONTEXT_REFRESHED_LOG_MSG = "Context refreshed.";

    private final ContextRefresher refresher;

    @Override
    public void onAdd(Secret secret) {
        log.debug(SECRET_ADDED_LOG_MSG, secret.getMetadata().getName());
        runRefreshEvent();
    }

    @Override
    public void onUpdate(Secret oldSecret, Secret newSecret) {
        log.debug(SECRET_UPDATED_LOG_MSG, newSecret.getMetadata().getName());
        if (Objects.equals(oldSecret.getData(), newSecret.getData())) {
            log.debug(SECRET_DATA_UNCHANGED_LOG_MSG);
        } else {
            runRefreshEvent();
        }
    }

    @Override
    public void onDelete(Secret secret, boolean deletedFinalStateUnknown) {
        log.debug(SECRET_WAS_DELETED_LOG_MSG, secret.getMetadata().getName());
    }

    private void runRefreshEvent() {
        CompletableFuture.runAsync(() -> {
            log.info(START_REFRESH_LOG_MSG);
            refresher.refresh();
            log.info(CONTEXT_REFRESHED_LOG_MSG);
        });
    }
}
