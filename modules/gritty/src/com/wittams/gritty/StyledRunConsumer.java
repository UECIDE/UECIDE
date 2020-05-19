package com.wittams.gritty;

public interface StyledRunConsumer {
	void consumeRun(int x, int y, Style style, char[] buf, int start, int len);
}
