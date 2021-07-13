package me.cheracc.battlegameshungerroyale.types;

public class PlayerSettings {
    private boolean alwaysShowScoreboard = true;
    private boolean showHelp = true;
    private String defaultKit = null;

    public void toggleScoreboard() { alwaysShowScoreboard = !alwaysShowScoreboard; }

    public boolean isAlwaysShowScoreboard() { return alwaysShowScoreboard; }

    public void toggleShowHelp() { showHelp = !showHelp; }

    public boolean isShowHelp() { return showHelp; }
}
