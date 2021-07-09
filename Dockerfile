FROM navikt/java:16
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-journalforing
COPY build/libs/app.jar app.jar