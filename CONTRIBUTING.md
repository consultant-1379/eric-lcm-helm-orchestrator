#Contributing to Helmfile Executor

This document describes how to contribute artifacts for the Helmfile Executor service.
When contributing to this repository, please first discuss the change you wish to make via JIRA issue, email, or any 
other method with the guardians of this repository before making a change.
The following is a set of guidelines for contributing to the Helmfile Executor project. These are mostly guidelines, not rules.
Use your best judgment, and feel free to contact the guardians if you have any questions.

###Project Guardians
The guardians are the maintainers of this project. Their responsibility is to guide the contributors and review the submitted patches.
- **Sunflowers team:** PDLSUNFLOW@pdl.internal.ericsson.com
- **Product owner:** illia.korchan.ext@ericsson.com

###How can I use this repository?
This repository contains the source code of the Helmfile Executor including functional and test code, documentation, and configuration files.
If you want to fix a bug or just want to experiment with adding a feature, you should try the service in your environment using a local copy of the project's source.

You can start by cloning the GIT repository to get your local copy:
```bash
git clone ssh://{USER}@gerrit.ericsson.se:29418/OSS/com.ericsson.orchestration.mgmt/eric-lcm-helm-orchestrator
```
###Development Environment prerequisites
The development work of Helmfile Executor is done in Java.
To be able to run the code and tests project locally you need to **change profiles.active to "local"** and start the postgres container:
```bash
docker run -e POSTGRES_PASSWORD=postgres -p 8200:5432 -d postgres
```
the following tools need to exist on the host:
- Java 17
- Docker
- Postgres
- Helm

###How Can I Contribute?
- Suggestion, including completely new features and minor improvements to existing functionality.
- Submitting a bug report for the Helmfile Executor. Please use this [Jira ticket](https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/CAM-721)
as template for creating a bug. It helps maintainers understand your report, reproduce the bug, and fix the issue promptly.


###CI Pipeline
Please find all [Jenkins pipelines](https://fem4s11-eiffel052.eiffel.gic.ericsson.se:8443/jenkins/view/CN-AM/) related to the Helmfile Executor.

**CN-AM_Lcm-Helm-Executor_PreCodeReview** is the underlying pipeline that triggers every time a patchset is pushed to the Gerrit.

**CN-AM_CI_Test** is a pipeline responsible for execution integration tests and can help to verify that provided changes
have not broken the functionality.

###Git Flow
Please follow the steps bellow to commit and push your changes to the Helmfile Executor properly: 
1. Checkout on remote master. This must be done before starting the implementation of a new task;
2. Fetch last changes from master:
 - when you don`t have any commits with the changes ```git pull```; 
 - when you have a commit with your changes ```git pull --rebase```;
 - when you downloaded some patchset from Gerrit and HEAD points to a particular commit ```git pull --rebase origin master```; 
3. Create new commit;
4. Add some changes to the existing commit with **Amend** flag;
5. Push your commit to Gerrit ```git push origin HEAD:refs/for/master```;

There are several rules to create a commit message:
- Use present tense imperative.
- Subject line less than or equal to 80 characters in length
- Commit message must start with the number of the ticket **(e.g. CAM-123)** or in case the commit is not related to any 
tickets you must specify **NoJira**.
