FROM eclipse-temurin:17-jre-alpine

LABEL description="Basic Proxy Facade for NTLM, Kerberos, SOCKS and Proxy Auto Config file proxies"
LABEL maintainer="ecovaci"

RUN mkdir /opt/winfoom

ADD target/winfoom.jar /opt/winfoom/winfoom.jar

ADD docker-entrypoint.sh /opt/winfoom/docker-entrypoint.sh

RUN chmod +x /opt/winfoom/docker-entrypoint.sh

EXPOSE 3129 9999

RUN addgroup -S winfoom && adduser -S -g winfoom winfoom

RUN mkdir /data && chown winfoom:winfoom /data && chown -R winfoom:winfoom /opt/winfoom

USER winfoom

VOLUME /data

ENV WINFOOM_CONFIG=/data
ENV FOOM_API_DISABLE_SHUTDOWN=true

WORKDIR /opt/winfoom

ENTRYPOINT [ "/opt/winfoom/docker-entrypoint.sh" ]