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

package com.ericsson.oss.management.lcm.model.entity;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name = "workload_instance")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class WorkloadInstance {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(unique = true, nullable = false, length = 64, name = "id")
    private String workloadInstanceId;

    @Column(name = "name")
    private String workloadInstanceName;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "crd_namespace")
    private String crdNamespace;

    @Column(name = "cluster")
    private String cluster;

    @ToString.Exclude
    @Column(name = "additional_parameters")
    private String additionalParameters;

    @Column(name = "latest_operation_id")
    private String latestOperationId;

    @Column(name = "previous_version")
    private Integer previousVersion;

    @Column(name = "version")
    private Integer version;

    @Column(name = "cluster_identifier")
    private String clusterIdentifier;

    @OneToOne(mappedBy = "workloadInstance")
    private ClusterConnectionInfoInstance clusterConnectionInfoInstance;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workloadInstance")
    private List<HelmSource> helmSources;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workloadInstance")
    private List<Operation> operations;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workloadInstance")
    private List<WorkloadInstanceVersion> workloadInstanceVersions;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workloadInstance")
    private List<Release> releases;

}
