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

package contracts.api.putHelmfileBuilderWorkloadInstance.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Updating a Workload Instance through helmfile builder. Updating means scale, upgrade, rollback, etc

```
given:
  client requests to update a Workload instance with incorrect instanceId
when:
  an instance not found
then:
  the request is rejected with 404 Not Found
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/helmfile_builder/workload_instances/not_existed_id"
        multipart(
                workloadInstanceWithChartsPutRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-data.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"Not Found",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail": "WorkloadInstance with id ${fromRequest().path(4)} not found",
                          "instance":""
                       }
                """
        )
    }
}

