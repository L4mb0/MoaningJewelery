package com.moaningjewelry;

import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.MenuEntry;
import net.runelite.api.ChatMessageType;
import net.runelite.client.RuneLite;
import net.runelite.client.chat.ChatMessageManager;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import javax.sound.sampled.*;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
		name = "Moaning Jewelry",
		description = "A RuneLite plugin that makes your jewelry moan when rubbed... because God is dead",
		tags={"meme,jewelery,moan,ahegao"}
)
public class MJPlugin extends Plugin
{
	public static final File MOAN_FOLDER = new File(RuneLite.RUNELITE_DIR, "moaningjewelry");
	private static final Logger log = LoggerFactory.getLogger(MJPlugin.class);
	private static final Random random = new Random();
	private static final String SOUND_VERSION = "v2"; // Increment this when you update sounds

	@Inject
	private Client client;

	@Inject
	private MJConfig config;

	@Override
	protected void startUp() throws Exception
	{
		if (!MOAN_FOLDER.exists()) {
			MOAN_FOLDER.mkdirs(); // Confirm the folder exists
		}

		// Check if we need to update sounds
		File versionFile = new File(MOAN_FOLDER, ".version");
		boolean needsUpdate = true;

		if (versionFile.exists()) {
			try {
				String currentVersion = Files.readString(versionFile.toPath()).trim();
				needsUpdate = !SOUND_VERSION.equals(currentVersion);
			} catch (Exception e) {
				log.warn("Could not read version file, will update sounds", e);
			}
		}

		if (needsUpdate) {
			log.info("Updating sound files to version: {}", SOUND_VERSION);
			copyResourceSoundsToFolder();

			// Write new version
			try {
				Files.writeString(versionFile.toPath(), SOUND_VERSION);
			} catch (Exception e) {
				log.warn("Could not write version file", e);
			}
		} else {
			log.info("Sound files are up to date (version: {})", SOUND_VERSION);
		}
	}

	private void copyResourceSoundsToFolder() {
		// List of sound files based on your file tree
		String[] soundFiles = {
				"1.wav", "2.wav", "3.wav", "4.wav", "5.wav",
				"6.wav", "7.wav", "8.wav", "9.wav", "10.wav", "11.wav"
		};

		for (String soundFileName : soundFiles) {
			File soundFile = new File(MOAN_FOLDER, soundFileName);

			// Force re-copy by deleting existing file first
			if (soundFile.exists()) {
				boolean deleted = soundFile.delete();
				log.info("Deleted existing cached file {}: {}", soundFileName, deleted);
			}

			try (InputStream inputStream = getClass().getResourceAsStream("/moans/" + soundFileName)) {
				if (inputStream != null) {
					Files.copy(inputStream, soundFile.toPath());
					log.info("Copied sound file: {}", soundFileName);
				} else {
					log.warn("Sound file not found in resources: {}", soundFileName);
				}
			} catch (Exception e) {
				log.error("Error copying sound file: {}", soundFileName, e);
			}
		}

		// Log the final contents of the folder for debugging
		File[] copiedFiles = MOAN_FOLDER.listFiles((dir, name) ->
				name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".mp3"));

		if (copiedFiles != null && copiedFiles.length > 0) {
			log.info("Successfully copied {} sound files to moan folder", copiedFiles.length);
			for (File file : copiedFiles) {
				log.info("Available sound file: {}", file.getName());
			}
		} else {
			log.warn("No sound files found in moan folder after copying!");
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Plugin stopped... thank God...");
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		// Debug: Log all menu options to see what's being detected
		log.debug("Menu option detected: '{}' on target: '{}'",
				event.getOption(), event.getTarget());

		// Check if the menu option is "Rub"
		if (event.getOption().equals("Rub")) {
			log.info("Found 'Rub' option! Changing to 'Bad Touch'");

			// Get the menu entries
			MenuEntry[] entries = client.getMenuEntries();

			// Modify the last menu entry (which corresponds to the event)
			MenuEntry lastEntry = entries[entries.length - 1];
			lastEntry.setOption("Bad Touch"); // Change "Rub" to "Bad Touch"

			// Set the modified entries back
			client.setMenuEntries(entries);
		}
	}

	private void sendChatMessage(String message) {
		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				message,
				null
		);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		// Debug: Log all menu clicks
		log.debug("Menu clicked: '{}' on target: '{}'",
				event.getMenuOption(), event.getMenuTarget());

		if (event.getMenuOption().equals("Bad Touch")) { // Match "Bad Touch" menu option
			log.info("Bad Touch clicked! Playing sound...");
			sendChatMessage("YAMETE KUDASAI!!!");
			if (config.soundEnabled()) {  // Check if sound is enabled in the config
				playRandomSound();
			} else {
				log.info("Sound is disabled in config");
			}
		}
	}

	private void playRandomSound() {
		// Get all .wav files from the moan folder
		File[] soundFiles = MOAN_FOLDER.listFiles((dir, name) ->
				name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".mp3"));

		if (soundFiles == null || soundFiles.length == 0) {
			log.warn("No sound files found in moan folder: {}", MOAN_FOLDER.getAbsolutePath());
			return;
		}

		// Try to play a random sound, with fallback to other files if one fails
		for (int attempts = 0; attempts < Math.min(5, soundFiles.length); attempts++) {
			File randomSoundFile = soundFiles[random.nextInt(soundFiles.length)];
			log.info("Attempting to play random sound: {}", randomSoundFile.getName());

			if (playSound(randomSoundFile)) {
				return; // Successfully played sound
			}
		}

		log.error("Failed to play any sound file after {} attempts", Math.min(5, soundFiles.length));
	}

	private boolean playSound(File soundFile) {
		if (!soundFile.exists()) {
			log.warn("Sound file not found: {}", soundFile.getAbsolutePath());
			return false;
		}

		try (InputStream fileStream = new BufferedInputStream(new FileInputStream(soundFile))) {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fileStream);

			// Log the audio format for debugging
			AudioFormat format = audioInputStream.getFormat();
			log.debug("Audio format for {}: {} Hz, {} bit, {} channels",
					soundFile.getName(), format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels());

			// Check if format is supported
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			if (!AudioSystem.isLineSupported(info)) {
				log.warn("Audio format not supported for file: {} ({})", soundFile.getName(), format);
				return false;
			}

			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);

			// Optional: Set volume control
			try {
				FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				if (volumeControl != null) {
					volumeControl.setValue(-10.0f); // Reduce volume by 10 dB
				}
			} catch (Exception e) {
				// Volume control not supported, continue without it
			}

			// Add a listener to release resources when the sound finishes
			clip.addLineListener(event -> {
				if (event.getType() == LineEvent.Type.STOP) {
					clip.close();
				}
			});

			clip.start(); // Play the sound
			log.info("Successfully started playing: {}", soundFile.getName());
			return true;

		} catch (UnsupportedAudioFileException e) {
			log.error("Unsupported audio file format: {}", soundFile.getName(), e);
			return false;
		} catch (LineUnavailableException e) {
			log.error("Audio line unavailable for file: {} - {}", soundFile.getName(), e.getMessage());
			return false;
		} catch (Exception e) {
			log.error("Error playing sound: {}", soundFile.getName(), e);
			return false;
		}
	}

	@Provides
	MJConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MJConfig.class);
	}
}