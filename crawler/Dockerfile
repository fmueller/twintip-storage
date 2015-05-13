FROM zalando/openjdk:8u40-b09-4

MAINTAINER Zalando SE

COPY target/twintip-crawler.jar /

CMD java $(java-dynamic-memory-opts) -jar /twintip-crawler.jar

ADD target/scm-source.json /scm-source.json
