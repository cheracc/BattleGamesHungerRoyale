package me.cheracc.battlegameshungerroyale.types.games;
import org.bukkit.Material;

public class GameType {
    private final String className;
    private final String prettyName;
    private final String description;
    private final Material icon;

    public GameType(String className, Material icon, String prettyName, String description) {
        this.className = className;
        this.icon = icon;
        this.prettyName = prettyName;
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }
}
