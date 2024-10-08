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

package contracts.api.getAllWorkloadInstances.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of listing Workload Instances with pagination

```
given:
  client requests to view Workload Instances on 2nd page(page numbers start from 1) with page size 2
when:
  a valid request is made
then:
  workload instances specified are displayed.
```

""")
    request {
        method 'GET'
        urlPath("/cnwlcm/v1/workload_instances") {
            queryParameters {
                parameter 'page': '2'
                parameter 'size': '2'
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file('pageTwoWorkloadInstancesAsc.json'))
        bodyMatchers {
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.first.href', byCommand("assertThat(parsedJson.read(\"\$._links.first\", Object.class)).isNotNull()"))
            jsonPath('$._links.prev.href', byCommand("assertThat(parsedJson.read(\"\$._links.prev\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.next.href', byCommand("assertThat(parsedJson.read(\"\$._links.next\", Object.class)).isNotNull()"))
            jsonPath('$._links.last.href', byCommand("assertThat(parsedJson.read(\"\$._links.last\", Object.class)).isNotNull()"))
            jsonPath('$._page', byCommand("assertThat(parsedJson.read(\"\$._page\", Object.class)).isNotNull()"))
            jsonPath('$._page.number', byCommand("assertThat(parsedJson.read(\"\$._page.number\", Object.class)).isNotNull()"))
            jsonPath('$._page.size', byCommand("assertThat(parsedJson.read(\"\$._page.size\", Object.class)).isNotNull()"))
            jsonPath('$._page.totalPages', byCommand("assertThat(parsedJson.read(\"\$._page.totalPages\", Object.class)).isNotNull()"))
            jsonPath('$._page.totalElements', byCommand("assertThat(parsedJson.read(\"\$._page.totalElements\", Object.class)).isNotNull()"))
            jsonPath('$.content', byCommand("assertThat(parsedJson.read(\"\$.content\", Object.class)).isNotNull()"))
        }
    }
}


