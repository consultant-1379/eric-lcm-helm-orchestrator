-- -----------------------------------------------------
-- ENUM `operation_state`
-- -----------------------------------------------------
create type operation_state as ENUM('PROCESSING', 'COMPLETED', 'FAILED');

-- -----------------------------------------------------
-- ENUM `operation_type`
-- -----------------------------------------------------
create type operation_type as ENUM('INSTANTIATE', 'UPDATE', 'REINSTANTIATE', 'ROLLBACK', 'TERMINATE');

-- -----------------------------------------------------
-- ENUM `helmsource_type`
-- -----------------------------------------------------
create type helmsource_type as ENUM('HELMFILE', 'INTEGRATION_CHART');

-- -----------------------------------------------------
-- ENUM `connection_info_status`
-- -----------------------------------------------------
create type connection_info_status as ENUM('IN_USE', 'NOT_IN_USE');

-- -----------------------------------------------------
-- Table `cluster_connection_info`
-- -----------------------------------------------------
CREATE TABLE cluster_connection_info (
    ID VARCHAR(250) NOT NULL UNIQUE,
    NAME VARCHAR(250) NOT NULL UNIQUE,
    URL VARCHAR(250) NOT NULL UNIQUE,
    CONTENT bytea,
    STATUS connection_info_status,
    CRD_NAMESPACE VARCHAR(250) DEFAULT NULL
);

-- -----------------------------------------------------
-- Table `workload_instance`
-- -----------------------------------------------------
CREATE TABLE workload_instance (
    ID VARCHAR(250) UNIQUE NOT NULL,
    NAME VARCHAR NOT NULL UNIQUE,
    NAMESPACE VARCHAR NOT NULL,
    CRD_NAMESPACE VARCHAR DEFAULT NULL,
    CLUSTER VARCHAR DEFAULT NULL,
    ADDITIONAL_PARAMETERS VARCHAR DEFAULT NULL,
    LATEST_OPERATION_ID VARCHAR(250) DEFAULT NULL,
    PREVIOUS_VERSION INT DEFAULT NULL,
    VERSION INT DEFAULT NULL
);

-- -----------------------------------------------------
-- Table `helmsource`
-- -----------------------------------------------------
CREATE TABLE helmsource (
    ID VARCHAR(250) NOT NULL UNIQUE,
    CONTENT BYTEA,
    TYPE helmsource_type,
    CREATED TIMESTAMP NOT NULL,
    WORKLOAD_INSTANCE_ID VARCHAR(250) NOT NULL,
    HELM_SOURCE_VERSION VARCHAR(250),
    FOREIGN KEY (WORKLOAD_INSTANCE_ID) REFERENCES WORKLOAD_INSTANCE (ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- -----------------------------------------------------
-- Table `operation`
-- -----------------------------------------------------
CREATE TABLE operation (
    ID VARCHAR(250) NOT NULL UNIQUE,
    HELMSOURCE_ID VARCHAR(250),
    STATE operation_state,
    TYPE operation_type,
    START_TIME TIMESTAMP NOT NULL,
    OUTPUT VARCHAR DEFAULT NULL,
    WORKLOAD_INSTANCE_ID VARCHAR(250) NOT NULL,
    TIMEOUT SMALLINT NOT NULL,
    FOREIGN KEY (WORKLOAD_INSTANCE_ID) REFERENCES WORKLOAD_INSTANCE (ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- -----------------------------------------------------
-- Table `cluster_connection_info_instance`
-- -----------------------------------------------------
CREATE TABLE cluster_connection_info_instance (
    ID VARCHAR(250) NOT NULL UNIQUE,
    WORKLOAD_INSTANCE_ID VARCHAR(250) NOT NULL UNIQUE,
    CLUSTER_CONNECTION_INFO_ID VARCHAR(250) NOT NULL,
    FOREIGN KEY (WORKLOAD_INSTANCE_ID) REFERENCES WORKLOAD_INSTANCE (ID) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (CLUSTER_CONNECTION_INFO_ID) REFERENCES CLUSTER_CONNECTION_INFO (ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- -----------------------------------------------------
-- Table `values`
-- -----------------------------------------------------
CREATE TABLE values (
    ID VARCHAR(250) NOT NULL UNIQUE,
    NAME VARCHAR(250) NOT NULL,
    CONTENT BYTEA NOT NULL
);

-- -----------------------------------------------------
-- Table `workload_instance_version`
-- -----------------------------------------------------
CREATE TABLE workload_instance_version (
    ID VARCHAR(250) UNIQUE NOT NULL,
    WORKLOAD_INSTANCE_ID VARCHAR(250) NOT NULL,
    VERSION INT NOT NULL,
    HELM_SOURCE_VERSION VARCHAR(250) NOT NULL,
    VALUES_VERSION VARCHAR(250) NOT NULL,
    FOREIGN KEY (WORKLOAD_INSTANCE_ID) REFERENCES WORKLOAD_INSTANCE (ID) ON DELETE RESTRICT ON UPDATE RESTRICT
);