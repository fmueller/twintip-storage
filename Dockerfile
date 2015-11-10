FROM zalando/openjdk:8u66-b17-1-2

MAINTAINER Zalando SE

COPY target/twintip-storage.jar /

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $(java-dynamic-memory-opts) $(newrelic-agent) $(appdynamics-agent) -jar /twintip-storage.jar

ADD target/scm-source.json /scm-source.json
