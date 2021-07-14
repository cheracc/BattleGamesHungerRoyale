package me.cheracc.battlegameshungerroyale.types;

public class PlayerSettings {
    private boolean showMainScoreboard = true;
    private boolean showGameScoreboard = true;
    private boolean showHelp = true;
    private String defaultKit = null;

    public void toggleMainScoreboard() { showMainScoreboard = !showMainScoreboard; }

    public void toggleGameScoreboard() {
        showGameScoreboard = !showGameScoreboard;
    }

    public boolean isShowMainScoreboard() { return showMainScoreboard; }

    public boolean isShowGameScoreboard() {
        return showGameScoreboard;
    }

    public void setShowHelp(boolean value) { showHelp = value; }

    public void setShowScoreboard(boolean value) {
        showMainScoreboard = value;
    }

    public boolean isShowHelp() { return showHelp; }

    public String getDefaultKit() {
        return defaultKit;
    }

    public void setDefaultKit(String value) {
        defaultKit = value;
    }
}
