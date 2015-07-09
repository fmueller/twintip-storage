FROM zalando/openjdk:8u45-b14-2

MAINTAINER Zalando SE

COPY target/twintip-storage.jar /

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $(java-dynamic-memory-opts) $(newrelic-agent) -jar /twintip-storage.jar

ADD target/scm-source.json /scm-source.json
