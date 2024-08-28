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

package contracts.api.postInstantiateWorkload.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Instantiating a Workload Instance

```
given:
  client requests to instantiate a Workload instance with invalid Json part
when:
  an invalid request is submitted
then:
  the request is rejected with 400 Bad Request
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/workload_instances"
        multipart(
                workloadInstancePostRequestDto: named(
                        name: value('request.json'),
                        content: value(null),
                        contentType: value("application/json")
                ),
                helmSource: named(
                        name: value('helmFile.yaml'),
                        // File extension couldn't be yaml as then it was processed as a contract
                        content: $(consumer(nonEmpty()), producer(file('helmFile.yaml.properties')))
                )
        )
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"Bad Request",
                          "title":"Malformed Request",
                          "status":400,
                          "detail":"Required part 'workloadInstancePostRequestDto' is not present.",
                          "instance":""
                       }
                """
        )
    }
}

