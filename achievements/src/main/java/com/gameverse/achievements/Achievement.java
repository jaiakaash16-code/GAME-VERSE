package com.gameverse.achievements;

/**
 * Represents an achievement that players can unlock.
 */
public class Achievement {
    
    private String id;
    private String name;
    private String description;
    private String icon;
    private int rewardXp;
    private int rewardCoins;
    
    public Achievement(String id, String name, String description, int rewardXp, int rewardCoins) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewardXp = rewardXp;
        this.rewardCoins = rewardCoins;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getRewardXp() {
        return rewardXp;
    }
    
    public int getRewardCoins() {
        return rewardCoins;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    @Override
    public String toString() {
        return String.format(
            "Achievement{id='%s', name='%s', rewardXp=%d, rewardCoins=%d}",
            id, name, rewardXp, rewardCoins
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Achievement)) return false;
        Achievement that = (Achievement) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
