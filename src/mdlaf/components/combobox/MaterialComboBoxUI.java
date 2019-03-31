package mdlaf.components.combobox;

import mdlaf.animation.MaterialUIMovement;
import mdlaf.utils.MaterialBorders;
import mdlaf.utils.MaterialDrawingUtils;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

public class MaterialComboBoxUI extends BasicComboBoxUI {

	public static ComponentUI createUI (JComponent c) {
		return new MaterialComboBoxUI ();
	}

	@Override
	public void installUI (JComponent c) {
		super.installUI (c);

		JComboBox<?> comboBox = (JComboBox<?>) c;
		comboBox.setFont (UIManager.getFont ("ComboBox.font"));
		comboBox.setBackground (UIManager.getColor ("ComboBox.background"));
		comboBox.setForeground (UIManager.getColor ("ComboBox.foreground"));
		comboBox.setBorder (UIManager.getBorder ("ComboBox.border"));
		comboBox.setLightWeightPopupEnabled (true);
		comboBox.setRenderer (new MaterialComboBoxRenderer ());
		comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	protected JButton createArrowButton () {
		Icon icon = UIManager.getIcon ("ComboBox.buttonIcon");
		JButton button;
		if (icon != null) {
			button = new JButton (icon);
		}
		else {
			button = new BasicArrowButton (SwingConstants.SOUTH);
		}
		button.setOpaque (true);
		button.setBackground (UIManager.getColor ("ComboBox.buttonBackground"));
		if(UIManager.getBoolean("ComboBox.mouseHoverEnabled")){
			button.addMouseListener(MaterialUIMovement.getMovement(button, UIManager.getColor ("ComboBox.mouseHoverColor")));
		}
		button.setBorder (MaterialBorders.LIGHT_LINE_BORDER);
		return button;
	}

	@Override
	public void paint (Graphics g, JComponent c) {
		super.paint (MaterialDrawingUtils.getAliasedGraphics (g), c);
	}


}
