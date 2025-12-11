package com.tonic.plugins.autoquester;

public enum AutoQuesterQuest
{
    NONE("None"),
    COOKS_ASSISTANT("Cook's Assistant"),
    SHEEP_SHEARER("Sheep Shearer");

    private final String displayName;

    AutoQuesterQuest(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
