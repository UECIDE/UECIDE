package edu.mit.blocks.workspace;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;

import edu.mit.blocks.codeblockutil.CSlider;

/**
 * A ZoomSlider manages the zooming of the block canvas
 * and all its children.
 * 
 * It interfaces with the user through a JSlider but upon
 * observing a change, it invokes the block canvas through
 * the Workspace to reset the zoom factor.
 */
public class ZoomSlider extends JComponent implements PropertyChangeListener {

    private static final long serialVersionUID = 328149080276L;
    /**JSlider that interfaces with the user**/
    private CSlider slider;
    private final Workspace workspace;

    /**
     * Constructs a new ZoomSlider
     */
    public ZoomSlider(Workspace workspace) {
        super();
        this.workspace = workspace;
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(200, 24));
        slider = new CSlider(34, 200, 100, true, 10, true, "100%");
        JLabel label = new JLabel("Zoom  ");
        label.setFont(new Font("Ariel", Font.BOLD, 10));
        label.setForeground(Color.white);
        this.add(label, BorderLayout.WEST);
        this.add(slider, BorderLayout.CENTER);
        slider.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(CSlider.VALUE_CHANGED)) {
            workspace.setWorkspaceZoom(slider.getValue() / 100.0);
        } else {
            slider.setValue(slider.getValue());
        }
        PageChangeEventManager.notifyListeners();
    }

    public void reset() {
        slider.setValue(100);
    }
}
