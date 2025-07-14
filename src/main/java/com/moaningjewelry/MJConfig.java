package com.moaningjewelry;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("moaningjewelry")
public interface MJConfig extends Config
{
	@ConfigItem(
		keyName = "soundEnabled",
		name = "Enable sound",
		description = "Enable or disable the moans... why are you doing this?",
		position = 1
	)
	default boolean soundEnabled(){
		return true; //default to enabled
	}
}
