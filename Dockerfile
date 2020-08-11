FROM openjdk:11
COPY . /metadata-sync
RUN /metadata-sync/gradlew --no-daemon -p /metadata-sync installDist
ENTRYPOINT ["/metadata-sync/build/install/metadata-sync/bin/metadata-sync"]
