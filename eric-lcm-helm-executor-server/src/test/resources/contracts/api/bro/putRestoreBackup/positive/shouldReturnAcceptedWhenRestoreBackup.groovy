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

package contracts.api.bro.putRestoreBackup.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Restore Backup 

```
given:
  client requests Restore operation
when:
  a valid request is submitted
then:
  the response is returned
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/backup_and_restore"
        headers {
            contentType(applicationJson())
        }
        body(file("bro_request.json"))
    }
    response {
        status ACCEPTED()
        body("""
{
    "id" : "123456"
}
            """
        )
    }
}

