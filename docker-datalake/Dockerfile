FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-11-runtime:1.14-3-03082022
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

RUN microdnf install unzip

# install the datalake app
ADD ${REPO_URL}/com/sequenceiq/datalake/$VERSION/datalake-$VERSION.jar /datalake.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.16.1.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip datalake.jar schema/* -d / ) || \
    ( unzip datalake.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Datalake application
COPY bootstrap/start_datalake_app.sh /
COPY bootstrap/wait_for_datalake_api.sh /

ENTRYPOINT ["/start_datalake_app.sh"]
