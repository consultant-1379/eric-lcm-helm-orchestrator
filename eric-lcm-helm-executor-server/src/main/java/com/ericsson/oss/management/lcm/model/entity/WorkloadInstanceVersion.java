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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "workload_instance_version")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class WorkloadInstanceVersion {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(unique = true, nullable = false, length = 64, name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "workload_instance_id", nullable = false)
    private WorkloadInstance workloadInstance;

    @Column(name = "version")
    private Integer version;

    @Column(name = "helm_source_version")
    private String helmSourceVersion;

    @Column(name = "values_version")
    private String valuesVersion;

    @Column(name = "service_name_identifier")
    private String serviceNameIdentifier;
}
