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

package com.ericsson.oss.management.lcm.presentation.controllers;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.ericsson.oss.management.lcm.api.ClusterConnectionInfoApi;
import com.ericsson.oss.management.lcm.api.model.ClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.api.model.PagedClusterConnectionInfoDto;
import com.ericsson.oss.management.lcm.presentation.services.clusterconnectioninfo.ClusterConnectionInfoService;
import com.ericsson.oss.management.lcm.presentation.services.coordinator.ClusterConnectionInfoRequestCoordinatorService;

@Slf4j
@RestController
@RequestMapping("/cnwlcm/v1")
@RequiredArgsConstructor
public class ClusterConnectionInfoController implements ClusterConnectionInfoApi {

    private final ClusterConnectionInfoService clusterConnectionInfoService;
    private final ClusterConnectionInfoRequestCoordinatorService clusterConnectionInfoRequestCoordinatorService;

    @Override
    public ResponseEntity<ClusterConnectionInfoDto> clusterConnectionInfoPost(final MultipartFile clusterConnectionInfo,
                                                                              @Valid String crdNamespace) {
        ClusterConnectionInfoDto response = clusterConnectionInfoRequestCoordinatorService.create(clusterConnectionInfo, crdNamespace);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> clusterConnectionInfoClusterConnectionInfoIdDelete(final String clusterConnectionInfoId) {
        clusterConnectionInfoService.delete(clusterConnectionInfoId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<ClusterConnectionInfoDto> clusterConnectionInfoClusterConnectionInfoIdGet(final String clusterConnectionInfoId) {
        ClusterConnectionInfoDto response = clusterConnectionInfoService.get(clusterConnectionInfoId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedClusterConnectionInfoDto> clusterConnectionInfoGet(@Valid @RequestParam(required = false) final Integer page,
                                                                   @Valid @RequestParam(required = false) final Integer size,
                                                                   @Valid @RequestParam(required = false) final List<String> sort) {
        log.info("Received a Get request to get all paged cluster connection info");
        PagedClusterConnectionInfoDto response = clusterConnectionInfoService.getAllClusterConnectionInfo(page, size, sort);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
