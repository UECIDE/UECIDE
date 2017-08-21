/**
 * 
 */
package com.wittams.gritty.swing;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

import com.wittams.gritty.swing.GrittyTerminal.BufferType;


public class BufferPanel extends JPanel {
	public BufferPanel(final GrittyTerminal terminal){
		super(new BorderLayout());
		final JTextArea area = new JTextArea();
		add(area, BorderLayout.NORTH);
		
		final BufferType[] choices = BufferType.values(); 
		
		final JComboBox chooser = new JComboBox(choices);
		add(chooser, BorderLayout.NORTH);
		
		area.setFont(Font.decode("Monospaced-14"));
		add(new JScrollPane(area), BorderLayout.CENTER);
		
		class Updater implements ActionListener, ItemListener{
			void update(){
				final BufferType type = (BufferType) chooser.getSelectedItem();
				final String text = terminal.getBufferText(type);
				area.setText(text);
			}
			
			public void actionPerformed(final ActionEvent e) {
				update();
			}

			public void itemStateChanged(final ItemEvent e) {
				update();
			}
		}
		final Updater up = new Updater();
		chooser.addItemListener(up);
		final Timer timer = new Timer(1000, up);
		timer.setRepeats(true);
		timer.start();
		
	}
}