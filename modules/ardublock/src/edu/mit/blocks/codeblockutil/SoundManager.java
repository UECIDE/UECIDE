package edu.mit.blocks.codeblockutil;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Manages the sounds for StarLogoBlocks */
public class SoundManager {

    private static boolean enableSound = true;

    public static Sound loadSound(String soundFileName) {
        final URL url = SoundManager.class.getResource(soundFileName);
        if (url == null) {
            System.out.println("Could not find resource " + soundFileName);
            return null;
        }

        AudioInputStream audioInputStream;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(url);
        } catch (UnsupportedAudioFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }

        final AudioFormat format = audioInputStream.getFormat();

        final Clip clip;
        try {
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioInputStream);
        } catch (LineUnavailableException e) {
            System.out.println("Sorry, sound is not available");
            return null;
        } catch (IOException e) {
            return null;
        }

        return new Sound(clip);
    }

    /**
     * Sets the ability to enable sound within the entire codeblocks library
     * If enableSound is set to false, no sounds can be played/heard until 
     * enableSound is set to true again.
     * @param enableSound
     */
    public static void setSoundEnabled(boolean enableSound) {
        SoundManager.enableSound = enableSound;
    }

    /**
     * Returns true iff sounds are being allowed to play.
     * @return true iff sounds are being allowed to play.
     */
    public static boolean isSoundEnabled() {
        return SoundManager.enableSound;
    }
}
