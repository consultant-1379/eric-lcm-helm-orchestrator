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

package contracts.api.bro.deleteBackup.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a scenario of Delete backup when Backup and Restore service is unavailable

```
given:
  client requests Delete backup
when:
  a valid request is submitted
then:
  the response is returned
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/backup_and_restore"
        headers {
            contentType(applicationJson())
        }
        body(file("bro_request.json"))
    }
    response {
        status SERVICE_UNAVAILABLE()
        body(
                """
{
    "type": "Service Unavailable",
    "title": "BackupAndRestoreConnectionException",
    "status": 503,
    "detail": "Backup and Restore Orchestrator is unavailable right now",
    "instance": ""
}
                """
        )
    }
}

