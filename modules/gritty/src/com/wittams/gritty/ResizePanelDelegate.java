package com.wittams.gritty;

import java.awt.Dimension;


public interface ResizePanelDelegate {

	void resizedPanel(Dimension pixelDimension, RequestOrigin origin);

}
