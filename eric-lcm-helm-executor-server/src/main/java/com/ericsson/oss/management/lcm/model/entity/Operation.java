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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "operation")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Operation {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "workload_instance_id", nullable = false)
    @ToString.Exclude
    private WorkloadInstance workloadInstance;

    @Column(name = "helmsource_id")
    private String helmSourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private OperationState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private OperationType type;

    @Column(name = "start_time")
    @CreationTimestamp
    private LocalDateTime startTime;

    @Column(name = "output")
    @Basic(fetch = FetchType.LAZY)
    @ToString.Exclude
    private String output;

    @Column(name = "timeout")
    private int timeout;

}
