ARG CBO_IMAGE_URL=armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles
ARG CBO_VERSION

FROM ${CBO_IMAGE_URL}:${CBO_VERSION}

ARG HELM_VERSION
ARG HELMFILE_VERSION
ARG HELM_DIFF_VERSION

ENV ARTIFACTORY_URL=https://arm.sero.gic.ericsson.se/artifactory/proj-eric-lcm-helm-executor-artifacts-generic-local

ADD ${ARTIFACTORY_URL}/packages/helm/${HELM_VERSION}/helm-${HELM_VERSION}-linux-amd64.tar.gz /
RUN tar -xf helm-${HELM_VERSION}-linux-amd64.tar.gz

ADD ${ARTIFACTORY_URL}/packages/helmfile/${HELMFILE_VERSION}/helmfile_${HELMFILE_VERSION}_linux_amd64.tar.gz /
RUN tar -zxf helmfile_${HELMFILE_VERSION}_linux_amd64.tar.gz

ADD ${ARTIFACTORY_URL}/plugins/helm-diff/${HELM_DIFF_VERSION}/helm-diff-linux-amd64.tgz /
RUN tar -xf helm-diff-linux-amd64.tgz

ADD ${ARTIFACTORY_URL}/rpm/inotify-tools/inotify-tools-3.20.2.2-17.1.x86_64.rpm /
ADD ${ARTIFACTORY_URL}/rpm/libinotifytools/libinotifytools0-3.20.2.2-17.1.x86_64.rpm /
RUN zypper --no-gpg-checks in -y /libinotifytools0-3.20.2.2-17.1.x86_64.rpm /inotify-tools-3.20.2.2-17.1.x86_64.rpm

COPY ./Docker/eric-lcm-helm-executor/script/ /script/
RUN chmod -R +x /script

FROM ${CBO_IMAGE_URL}:${CBO_VERSION}

ARG CBO_REPO=arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/
ARG CBO_VERSION=${CBO_VERSION}
ARG PRODUCT_VERSION
ARG COMMIT
ARG CONTAINER_ID
ARG PRODUCT_NUMBER
ARG PRODUCT_NAME
ARG BUILD_DATE
ARG HELM_VERSION
ARG HELMFILE_VERSION
ARG HELM_DIFF_VERSION

ENV CONTAINER_ID=${CONTAINER_ID}

LABEL \
    adp.commit=$COMMIT \
    adp.app.version=$PRODUCT_VERSION \
    com.ericsson.base-image.product-name="Common Base OS SLES IMAGE" \
    com.ericsson.base-image.product-number="CXC2012032" \
    com.ericsson.base-image.product-version="${CBO_VERSION}" \
    com.ericsson.product-number="$PRODUCT_NUMBER" \
    com.ericsson.product-name="$PRODUCT_NAME" \
    com.ericsson.product-revision="$PRODUCT_VERSION" \
    com.ericsson.product-3pp-app1-name="Helmfile" \
    com.ericsson.product-3pp-app1-version="${HELMFILE_VERSION}" \
    com.ericsson.product-3pp-app2-name="Helm" \
    com.ericsson.product-3pp-app2-version="${HELM_VERSION}" \
    org.opencontainers.image.title="$PRODUCT_NAME" \
    org.opencontainers.image.created="$BUILD_DATE" \
    org.opencontainers.image.revision="$COMMIT" \
    org.opencontainers.image.vendor="Ericsson" \
    org.opencontainers.image.version="$PRODUCT_VERSION"

RUN zypper addrepo --gpgcheck-strict -f https://${CBO_REPO}${CBO_VERSION} CBO_ENV \
    && zypper --gpg-auto-import-keys refresh \
    && zypper refresh \
    && zypper install -l -y java-17-openjdk-headless git-core \
    && zypper rr CBO_ENV \
    && zypper clean --all

RUN echo "${CONTAINER_ID}:x:${CONTAINER_ID}:" >> /etc/group \
    && echo "${CONTAINER_ID}:x:${CONTAINER_ID}:${CONTAINER_ID}:An identity for RA CNAM components:/home/${CONTAINER_ID}:/bin/false" >> /etc/passwd \
    && echo "${CONTAINER_ID}:!::0:::::" >> /etc/shadow \
    && mkdir /logs && chown $CONTAINER_ID:$CONTAINER_ID /logs

COPY --from=0 --chown=$CONTAINER_ID:$CONTAINER_ID /linux-amd64/helm /usr/local/bin/helm
COPY --from=0 --chown=$CONTAINER_ID:$CONTAINER_ID /helmfile /usr/local/bin/helmfile
COPY --from=0 --chown=$CONTAINER_ID:$CONTAINER_ID /diff /opt/diff/
COPY --from=0 /script/ /
COPY --from=0 /usr/lib64/libinotifytools.s* /usr/lib64/
COPY --from=0 /usr/bin/inotifywait /usr/bin/
COPY --chown=$CONTAINER_ID:$CONTAINER_ID ./eric-lcm-helm-executor-server/target/eric-lcm-helm-executor-server.jar /eric-lcm-helm-executor-server.jar

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 8888

USER $CONTAINER_ID

CMD [ "java" \
   , "-Xlog:age*=debug" \
   , "-Djava.security.egd=file:/dev/./urandom" \
   , "-jar" \
   , "/eric-lcm-helm-executor-server.jar" \
]
