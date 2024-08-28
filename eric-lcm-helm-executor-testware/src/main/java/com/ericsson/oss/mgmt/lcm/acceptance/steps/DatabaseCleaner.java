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
package com.ericsson.oss.mgmt.lcm.acceptance.steps;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DatabaseCleaner {
    private DatabaseCleaner(){}

    private static final String CLEAN_WORKLOAD_INSTANCE_VERSION_TABLE = "DELETE FROM public.workload_instance_version;";
    private static final String CLEAN_RELEASE_TABLE = "DELETE FROM public.release;";
    private static final String CLEAN_OPERATION_TABLE = "DELETE FROM public.operation;";
    private static final String CLEAN_HELMSOURCE_TABLE = "DELETE FROM public.helmsource;";
    private static final String CLEAN_CLUSTER_CONNECTION_INFO_INSTANCE_TABLE = "DELETE FROM public.cluster_connection_info_instance;";
    private static final String CLEAN_CLUSTER_CONNECTION_INFO_TABLE = "DELETE FROM public.cluster_connection_info;";
    private static final String CLEAN_WORKLOAD_INSTANCE_TABLE = "DELETE FROM public.workload_instance;";
    private static final String CLEAN_VALUES_TABLE = "DELETE FROM public.values;";
    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:8200/postgres";
    private static final String POSTGRES_CRED = "postgres";

    @Step("Clean database after tests with cluster-connection-info-local.config")
    public static void cleanDatabaseAfterTests() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(getDatasource());

        log.info("Will clean database after local tests");
        jdbcTemplate.execute(CLEAN_WORKLOAD_INSTANCE_VERSION_TABLE);
        jdbcTemplate.execute(CLEAN_RELEASE_TABLE);
        jdbcTemplate.execute(CLEAN_OPERATION_TABLE);
        jdbcTemplate.execute(CLEAN_HELMSOURCE_TABLE);
        jdbcTemplate.execute(CLEAN_CLUSTER_CONNECTION_INFO_INSTANCE_TABLE);
        jdbcTemplate.execute(CLEAN_CLUSTER_CONNECTION_INFO_TABLE);
        jdbcTemplate.execute(CLEAN_WORKLOAD_INSTANCE_TABLE);
        jdbcTemplate.execute(CLEAN_VALUES_TABLE);
    }

    private static DriverManagerDataSource getDatasource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(POSTGRES_URL);
        ds.setUsername(POSTGRES_CRED);
        ds.setPassword(POSTGRES_CRED);
        return ds;
    }
}
