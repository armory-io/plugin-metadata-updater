FROM amazoncorretto:24
COPY . /metadata-repo-updater
RUN /metadata-repo-updater/gradlew --no-daemon -p /metadata-repo-updater installDist
ENTRYPOINT ["/metadata-repo-updater/build/install/metadata-repo-updater/bin/metadata-repo-updater"]
