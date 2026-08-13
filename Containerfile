FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn --batch-mode -DskipTests dependency:go-offline
COPY src ./src
RUN mvn --batch-mode -DskipTests package

FROM registry.access.redhat.com/ubi9/nodejs-22:latest
USER 0
RUN dnf -y install java-21-openjdk-headless && dnf clean all

ARG CODEX_VERSION=0.147.0
RUN npm install -g "@openai/codex@${CODEX_VERSION}" \
    && codex --version \
    && npm cache clean --force

WORKDIR /opt/app-root/src
RUN mkdir -p /opt/app-root/src/.codex /workspaces \
    && chown -R 1001:0 /opt/app-root/src /workspaces \
    && chmod -R g=u /opt/app-root/src /workspaces

COPY --from=build --chown=1001:0 /workspace/target/codex-remote-gateway-*.jar /opt/app-root/src/app.jar

USER 1001
ENV HOME=/opt/app-root/src \
    CODEX_HOME=/opt/app-root/src/.codex \
    GATEWAY_AGENT_MODE=codex \
    GATEWAY_CODEX_WORKSPACE_ROOT=/workspaces \
    GATEWAY_CODEX_HOME=/opt/app-root/src/.codex

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/app-root/src/app.jar"]
