package net.runelite.client.plugins.projectx.wildernessagility;

import lombok.Getter;

public class WildernessAgilityObstacleModel {
    @Getter
    private final int objectId;
    @Getter
    private final boolean canFail;

    public WildernessAgilityObstacleModel(int objectId, boolean canFail) {
        this.objectId = objectId;
        this.canFail = canFail;
    }
} 