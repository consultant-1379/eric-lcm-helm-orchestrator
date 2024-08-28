ARG CBO_IMAGE_URL=armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles
ARG CBO_VERSION

FROM ${CBO_IMAGE_URL}:${CBO_VERSION}

ARG BUILD_DATE
ARG CBO_REPO=arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/
ARG CBO_VERSION=${CBO_VERSION}
ARG PRODUCT_VERSION
ARG COMMIT
ARG CONTAINER_ID
ARG POSTGRESQL_VERSION
ARG PRODUCT_NAME
ARG PRODUCT_NUMBER
ARG PRODUCT_NAME
ARG BUILD_DATE

LABEL \
    adp.commit=$COMMIT \
    adp.app.version=$PRODUCT_VERSION \
    com.ericsson.base-image.product-name="Common Base OS SLES IMAGE" \
    com.ericsson.base-image.product-number="CXC2012032" \
    com.ericsson.base-image.product-version="${CBO_VERSION}" \
    com.ericsson.product-number="$PRODUCT_NUMBER" \
    com.ericsson.product-name="$PRODUCT_NAME" \
    com.ericsson.product-revision="$PRODUCT_VERSION" \
    com.ericsson.product-3pp-app1-name="$POSTGRESQL_VERSION" \
    org.opencontainers.image.title="init_container_$PRODUCT_NAME" \
    org.opencontainers.image.created="$BUILD_DATE" \
    org.opencontainers.image.revision="$COMMIT" \
    org.opencontainers.image.vendor="Ericsson" \
    org.opencontainers.image.version="$PRODUCT_VERSION"

COPY ./ /

RUN zypper addrepo --gpgcheck-strict -f https://${CBO_REPO}${CBO_VERSION} CBO_ENV \
    && zypper --gpg-auto-import-keys refresh \
    && zypper refresh \
    && zypper install -l -y ${POSTGRESQL_VERSION} \
    && zypper rr CBO_ENV \
    && zypper clean --all \
    && chmod 555 /entrypoint.sh

RUN echo "${CONTAINER_ID}:x:${CONTAINER_ID}:" >> /etc/group \
    && echo "${CONTAINER_ID}:x:${CONTAINER_ID}:${CONTAINER_ID}:An identity for RA CNAM components:/home/${CONTAINER_ID}:/bin/false" >> /etc/passwd \
    && echo "${CONTAINER_ID}:!::0:::::" >> /etc/shadow

USER $CONTAINER_ID

ENTRYPOINT ["/entrypoint.sh"]
CMD [""]
