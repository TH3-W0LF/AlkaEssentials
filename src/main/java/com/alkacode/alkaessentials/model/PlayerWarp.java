package com.alkacode.alkaessentials.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/** Warp criado pelo proprio jogador (nao confundir com {@link Warp}, o warp global
 * criado por admin via /setwarp) - visibilidade publico/privado/whitelist, preco
 * opcional pra teleportar, categoria/descricao livres, avaliacao por estrelas. */
public final class PlayerWarp {

    public enum Visibility { PUBLIC, PRIVATE, WHITELIST }

    private final long id;
    private final UUID owner;
    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private String description;
    private String category;
    private Visibility visibility;
    private double price;
    private String currency;
    private String material;
    private final long createdAt;

    public PlayerWarp(long id, UUID owner, String name, String world, double x, double y, double z,
                       float yaw, float pitch, String description, String category, Visibility visibility,
                       double price, String currency, String material, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.description = description;
        this.category = category;
        this.visibility = visibility;
        this.price = price;
        this.currency = currency;
        this.material = material;
        this.createdAt = createdAt;
    }

    public long id() { return id; }
    public UUID owner() { return owner; }
    public String name() { return name; }
    public String description() { return description; }
    public String category() { return category; }
    public Visibility visibility() { return visibility; }
    public double price() { return price; }
    public String currency() { return currency; }
    public String material() { return material; }
    public long createdAt() { return createdAt; }

    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public void setPrice(double price) { this.price = price; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setMaterial(String material) { this.material = material; }

    /** Null se o mundo salvo nao esta carregado no momento. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z, yaw, pitch);
    }
}
