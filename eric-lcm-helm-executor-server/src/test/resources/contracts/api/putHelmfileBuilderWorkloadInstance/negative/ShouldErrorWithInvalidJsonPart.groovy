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
  client requests to update a Workload instance with invalid Json part
when:
  an invalid request is submitted
then:
  the request is rejected with 400 Bad Request
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/helmfile_builder/workload_instances/some_valid_id"
        multipart(
                workloadInstanceWithChartsPutRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('invalid-multipart-data.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"Bad Request",
                          "title":"Invalid Input Exception",
                          "status":400,
                          "detail":"Failed to parse json part of request due to Unrecognized token 'invalid': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\\n at [Source: (String)\\"invalid value\\"; line: 1, column: 8]",
                          "instance":""
                       }
                """
        )
    }
}
