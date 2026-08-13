package com.alkacode.alkaessentials.model;

import java.util.UUID;

/** Uma punicao registrada no banco (tipo, alvo, autor, motivo, prova, duracao). */
public final class Punishment {

    public static final long PERMANENT = -1L;

    private int id;
    private final UUID target;
    private final String targetName;
    private final String type;
    private final String reason;
    private final String proof;
    private final String issuer;
    private final String server;
    private final long startTime;
    private final long endTime;
    private boolean active;

    public Punishment(int id, UUID target, String targetName, String type, String reason, String proof,
                      String issuer, String server, long startTime, long endTime, boolean active) {
        this.id = id;
        this.target = target;
        this.targetName = targetName;
        this.type = type;
        this.reason = reason;
        this.proof = proof;
        this.issuer = issuer;
        this.server = server;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public UUID getTarget() { return target; }
    public String getTargetName() { return targetName; }
    public String getType() { return type; }
    public String getReason() { return reason; }
    public String getProof() { return proof; }
    public String getIssuer() { return issuer; }
    public String getServer() { return server; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isPermanent() {
        return endTime == PERMANENT;
    }

    public boolean isExpired() {
        return !isPermanent() && System.currentTimeMillis() > endTime;
    }

    public boolean isBan() {
        return type.contains("BAN");
    }

    public boolean isMute() {
        return type.contains("MUTE");
    }

    public boolean isTemp() {
        return type.contains("TEMP");
    }

    public boolean isIp() {
        return type.contains("IP");
    }
}
