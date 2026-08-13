package io.github.son1004007.codexremote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private final Agent agent = new Agent();
    private final Codex codex = new Codex();

    public Agent getAgent() {
        return agent;
    }

    public Codex getCodex() {
        return codex;
    }

    public static class Agent {
        private String mode = "in-memory";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Codex {
        private String command = "codex";
        private String workspaceRoot = "/workspaces";
        private String codexHome = "";
        private String approvalPolicy = "never";
        private String sandbox = "workspace-write";
        private String model = "";
        private Duration turnTimeout = Duration.ofMinutes(10);

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getWorkspaceRoot() {
            return workspaceRoot;
        }

        public void setWorkspaceRoot(String workspaceRoot) {
            this.workspaceRoot = workspaceRoot;
        }

        public String getCodexHome() {
            return codexHome;
        }

        public void setCodexHome(String codexHome) {
            this.codexHome = codexHome;
        }

        public String getApprovalPolicy() {
            return approvalPolicy;
        }

        public void setApprovalPolicy(String approvalPolicy) {
            this.approvalPolicy = approvalPolicy;
        }

        public String getSandbox() {
            return sandbox;
        }

        public void setSandbox(String sandbox) {
            this.sandbox = sandbox;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getTurnTimeout() {
            return turnTimeout;
        }

        public void setTurnTimeout(Duration turnTimeout) {
            this.turnTimeout = turnTimeout;
        }
    }
}
