FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-11-runtime:1.14-3-03082022
MAINTAINER info@cloudera.com

WORKDIR /

ADD cloudbreak.jar /
ADD start_cloudbreak_app.sh /
ADD wait_for_cloudbreak_api.sh /
ADD jmx_prometheus_javaagent-0.10.jar /

# extract schema files
# extract schema files
RUN ( unzip cloudbreak.jar schema/* -d / ) || \
    ( unzip cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

ENTRYPOINT ["/start_cloudbreak_app.sh"]
