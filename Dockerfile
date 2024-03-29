FROM gcr.io/distroless/java17-debian11:latest
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-journalforing

COPY build/libs/app.jar /app/app.jar
WORKDIR /app
CMD [ "app.jar" ]
