
package uecide.plugin;

import uecide.app.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

//import uecide.app.tools.WavFile;
//import uecide.app.tools.WavFileException;


public class ConvertWaveFile extends BasePlugin {

    JFrame win;

    WavFile wav;

    JLabel inFileLabel;
    JLabel inFileName;
    JButton loadFileButton;
    File inFile;

    JCheckBox resample;
    JCheckBox merge;

    JLabel info;

    JComboBox<String> rates;
    JLabel newFilenameLabel;
    JTextArea newFilename;
    JLabel newFilenameSuffix;

    JButton convertButton;

    int resampleTo;

    public void init(Editor editor) {
        this.editor = editor;
        win = new JFrame("Convert WAV File");
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(true);

        File themeFolder = Base.getContentFile("lib/theme");

        Box box = Box.createVerticalBox();
        box.setBorder(new EmptyBorder(12, 12, 12, 12));


        Box line = Box.createHorizontalBox();
        inFileLabel = new JLabel("WAV File: ");
        inFileName = new JLabel("None Selected");

        inFileName.setPreferredSize(new Dimension(400, 20));
        inFileName.setMinimumSize(new Dimension(400, 20));
        inFileName.setMaximumSize(new Dimension(400, 20));
        inFileName.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        File openIconFile = new File(themeFolder, "open.png");
        ImageIcon openButtonIcon = new ImageIcon(openIconFile.getAbsolutePath());
        loadFileButton = new JButton(openButtonIcon);

        loadFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                selectWavFile();
            }
        });

        line.add(inFileLabel);
        line.add(inFileName);
        line.add(loadFileButton);
        box.add(line);

        line = Box.createHorizontalBox();
        info = new JLabel("");

        info.setPreferredSize(new Dimension(500, 20));
        info.setMinimumSize(new Dimension(500, 20));
        info.setMaximumSize(new Dimension(500, 20));

        line.add(info);
        box.add(line);

        line = Box.createHorizontalBox();
        merge = new JCheckBox("Merge to Mono     ");
        line.add(merge);
        resample = new JCheckBox("Resample to");
        line.add(resample);
        rates = new JComboBox<String>(new String[] {
            "8000", "11025", "16000", "22050", "32000", "44100"
        });

        resample.setEnabled(false);
        rates.setEnabled(false);
        resampleTo = 8000;
        rates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resampleTo = Integer.parseInt((String) rates.getSelectedItem());
            }
        });
        line.add(rates);
        box.add(line);

        line = Box.createHorizontalBox();
        newFilenameLabel = new JLabel("New Filename: ");
        line.add(newFilenameLabel);
        newFilename = new JTextArea("");

        newFilename.setPreferredSize(new Dimension(150, 20));
        newFilename.setMinimumSize(new Dimension(150, 20));
        newFilename.setMaximumSize(new Dimension(150, 20));
        newFilename.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        line.add(newFilename);

        newFilenameSuffix = new JLabel(".h         ");
        line.add(newFilenameSuffix);

        convertButton = new JButton("Convert");
        convertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                DefaultConvertHandler conv = new DefaultConvertHandler();
                Thread thr = new Thread(conv);
                thr.start();
            }
        });
        line.add(convertButton);

        box.add(line);

        win.getContentPane().add(box);

        win.pack();

        Dimension size = win.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        win.setLocation((screen.width - size.width) / 2,
                          (screen.height - size.height) / 2);

        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                win.setVisible(false);
            }
        });
        Base.registerWindowCloseKeys(win.getRootPane(), new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                win.setVisible(false);
            }
        });
        Base.setIcon(win);
    }

    public void selectWavFile()
    {
        // get the frontmost window frame for placing file dialog
        FileDialog fd = new FileDialog(this.editor,
            "Open WAV File",
            FileDialog.LOAD);

        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".wav");
            }
        });

        fd.setVisible(true);

        String directory = fd.getDirectory();
        String filename = fd.getFile();

        // User canceled selection
        if (filename == null) return;

        inFile = new File(directory, filename);
        if (!inFile.exists()) {
            return;
        }

        try {
            wav = WavFile.openWavFile(inFile);
            inFileName.setText(inFile.getAbsolutePath());
            info.setText("Channels: " + wav.getNumChannels() + ", " + wav.getSampleRate() + "Hz");
            merge.setSelected(wav.getNumChannels() == 2);
            String newname = inFile.getName();
            newname = newname.toLowerCase();
            newname = newname.substring(0, newname.length() - 4);
            newname = newname.replace(" ", "");
            newname = newname.replace(".", "");
            newname = newname.replace("-", "");
            newFilename.setText(newname);
            wav.close();
        } catch (Exception e) {
            System.err.println("Open Error: " + e.getMessage());
        }
    }

    public String getMenuTitle() {
        return "Convert WAV file";
    }

    public void run() {
        win.setVisible(true);
    }

    public int mix(int a, int b)
    {
        int z;
        int fa, fb, fz;
        fa = a + 32768;
        fb = b + 32768;

        if (fa < 32768 && fb < 32768) {
            fz = (fa * fb) / 32768;
        } else {
            fz = (2 * (fa + fb)) - ((fa * fb) / 32768) - 65536;
        }

        z = fz - 32768;
        return z;
    }

    class DefaultConvertHandler implements Runnable {
        public String inputFile;
        public String destination;
        public void run() {
            try {
                wav = WavFile.openWavFile(inFile);

                int chans = wav.getNumChannels();
                int[] buffer = new int[16 * chans];
                int framesRead;

                long numSamples = 0;

                editor.status.progress("Converting...");
                long totSamples = wav.getNumFrames();

                StringBuilder newText = new StringBuilder();

                newText.append("const short " + newFilename.getText() + "[] = {\n  ");

                do {
                    framesRead = wav.readFrames(buffer, 16);
                    for (int s=0; s<(framesRead * chans); s++) {
                        int sample;
                        if (chans == 2 && merge.isSelected()) {
                            sample = mix(buffer[s], buffer[s+1]);
                            s++;
                        } else {
                            sample = buffer[s];
                        }
                        newText.append(sample + ", "); 
                        numSamples++;
                    }
                    newText.append( "\n  ");
                    editor.status.progressUpdate((int) (numSamples * 100 / totSamples));
                    
                } while(framesRead != 0);
                newText.append("};\n");
                newText.append("const unsigned long " + newFilename.getText() + "_len = " + numSamples + ";\n\n");

                File newFile = new File(editor.getSketch().getFolder(), newFilename.getText() + ".h");
                PrintWriter pw = new PrintWriter(newFile);
                pw.print(newText.toString());
                pw.close();
                editor.addTab(newFile);
                editor.status.progressNotice("Converted");
                editor.status.unprogress();
                win.setVisible(false);
            } catch (Exception e) {
                System.err.println("Convert Error: " + e);
            }
        }
    }
}
