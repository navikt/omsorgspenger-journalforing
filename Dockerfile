FROM gcr.io/distroless/java21-debian12
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-journalforing

COPY build/libs/app.jar /app/app.jar
WORKDIR /app

USER nonroot

CMD [ "app.jar" ]
