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

package contracts.api.putUpdateWorkload.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Updating a Workload Instance. Updating means scale, upgrade, rollback, etc

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
        url "/cnwlcm/v1/workload_instances/not_existed_id"
        multipart(
                workloadInstancePutRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-data.json'))),
                        contentType: value("application/json")
                ),
                helmSource: named(
                        name: value('helmFile.yaml'),
                        // File extension couldn't be yaml as then it was processed as a contract
                        content: $(consumer(nonEmpty()), producer(file('helmFile.yaml.properties')))
                ),
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
                          "detail": "WorkloadInstance with id ${fromRequest().path(3)} not found",
                          "instance":""
                       }
                """
        )
    }
}

