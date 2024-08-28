-- -----------------------------------------------------
-- Update table `workload_instance`
-- -----------------------------------------------------
ALTER TABLE workload_instance
ADD COLUMN CLUSTER_IDENTIFIER VARCHAR DEFAULT NULL;