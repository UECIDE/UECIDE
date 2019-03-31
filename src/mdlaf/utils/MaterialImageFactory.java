/*
 * MIT License
 *
 * Copyright (c) 2018 Vincent Palazzo vincenzopalazzodev@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package mdlaf.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author https://github.com/vincenzopalazzo
 */
public class MaterialImageFactory {

    private static MaterialImageFactory SINGLETON;

    public static final String RIGHT_ARROW = "/imgs/right_arrow.png";
    public static final String DOWN_ARROW = "/imgs/down_arrow.png";
    public static final String UP_ARROW = "/imgs/up_arrow.png";
    public static final String PAINTED_BLACK_CHECKED_BOX = "/imgs/painted_checked_box.png";
    public static final String PAINTED_BLUE_CHECKED_BOX = "/imgs/blue-check-box-with-white-check.png";
    public static final String OUTLINED_CHECKED_BOX = "/imgs/outlined_checked_box.png";
    public static final String UNCHECKED_BOX = "/imgs/unchecked_box.png";
    public static final String RADIO_BUTTON_ON = "/imgs/radio_button_on.png";
    public static final String RADIO_BUTTON_OFF = "/imgs/radio_button_off.png";
    public static final String TOGGLE_BUTTON_ON = "/imgs/toggle_on.png";
    public static final String TOGGLE_BUTTON_OFF = "/imgs/toggle_off.png";
    public static final String BACK_ARROW = "/imgs/back_arrow.png";
    public static final String COMPUTER = "/imgs/computer.png";
    public static final String FILE = "/imgs/file.png";
    public static final String FLOPPY_DRIVE = "/imgs/floppy_drive.png";
    public static final String FOLDER = "/imgs/folder.png";
    public static final String HARD_DRIVE = "/imgs/hard_drive.png";
    public static final String HOME = "/imgs/home.png";
    public static final String LIST = "/imgs/list.png";
    public static final String NEW_FOLDER = "/imgs/new_folder.png";
    public static final String DETAILS = "/imgs/details.png";
    public static final String YES_COLLASSED = "/imgs/yes-collassed.png";
    public static final String NO_COLLASSED = "/imgs/no-collassed.png";
    public static final String ERROR = "/imgs/error.png";
    public static final String WARNING = "/imgs/warning.png";
    public static final String QUESTION = "/imgs/question.png";
    public static final String INFORMATION = "/imgs/information.png";

    private Map<String, BufferedImage> cachaImage = new HashMap<>();

    public static MaterialImageFactory getIstance() {
        if (SINGLETON == null) {
            SINGLETON = new MaterialImageFactory();
        }
        return SINGLETON;
    }

    private MaterialImageFactory() {
    }

    public BufferedImage getImage(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Argument nulled");
        }
        if (cachaImage.containsKey(key)) {
            return cachaImage.get(key);
        }
        try (InputStream inputStream = MaterialImages.class.getResourceAsStream(key)) {
            BufferedImage image = ImageIO.read(inputStream);
            cachaImage.put(key, image);
            return cachaImage.get(key);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Image " + key + " wasn't loaded");
        }
    }
}
