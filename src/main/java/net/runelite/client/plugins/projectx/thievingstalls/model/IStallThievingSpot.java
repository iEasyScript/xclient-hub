package net.runelite.client.plugins.projectx.thievingstalls.model;

public interface IStallThievingSpot {
    void thieve();
    void bank();

    Integer[] getItemIdsToDrop();
}
