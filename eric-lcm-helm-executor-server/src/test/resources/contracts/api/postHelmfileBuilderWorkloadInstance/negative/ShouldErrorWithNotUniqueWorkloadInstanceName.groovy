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

package contracts.api.postHelmfileBuilderWorkloadInstance.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Instantiating a Workload Instance via Helmfile Builder

```
given:
  client requests to instantiate a Workload instance with the name which is already present
when:
  an invalid request is submitted
then:
  the request is rejected with 409 Conflict
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/helmfile_builder/workload_instances"
        multipart(
                workloadInstanceWithChartsRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('not-unique-name-multipart-data.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"Conflict",
                          "title":"Not Unique WorkloadInstance Exception",
                          "status":409,
                          "detail":"WorkloadName not-unique-name exist. Name of the workloadInstance must be unique",
                          "instance":""
                       }
                """
        )
    }
}

