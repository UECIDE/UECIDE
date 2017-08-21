/**
 * 
 */
package com.wittams.gritty;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.WeakHashMap;

public class Style implements Cloneable{
	static int count = 1;
	
	static class ChosenColor extends Color{
		private static final long serialVersionUID = 7492667732033832704L;

		public ChosenColor(final Color def) {
			super(def.getRGB());
		}
	}
	
	static final ChosenColor FOREGROUND = new ChosenColor(Color.GREEN);
	static final ChosenColor BACKGROUND = new ChosenColor(Color.BLACK);
	
	public static enum Option{
		BOLD,
		BLINK,
		DIM,
		REVERSE, 
		UNDERSCORE,
		HIDDEN
	}
	
	public static final Style EMPTY = new Style();
	private static final WeakHashMap<Style, WeakReference<Style> > styles = new WeakHashMap<Style, WeakReference<Style>>();
	
	
	public static Style getCanonicalStyle(Style currentStyle) {
		final WeakReference<Style> canonRef = styles.get(currentStyle);
		if(canonRef != null){
			final Style canonStyle = canonRef.get();
			if(canonStyle != null){
				return canonStyle;
			}
		}
		styles.put(currentStyle , new WeakReference<Style>(currentStyle) );
		return currentStyle;
	}
	
	
	private Color foreground;
	private Color background;
	private EnumSet<Option> options;
	private int number; 
	
	Style(){
		number = count++;
		foreground = FOREGROUND;
		background = BACKGROUND;
		options = EnumSet.noneOf(Option.class); 
	}
	
	Style(final Color foreground, final Color background, final EnumSet<Option> options){
		number = count++;
		this.foreground = foreground;
		this.background = background;
		this.options = options.clone(); 
	}
	
	void setForeground(final Color foreground) {
		this.foreground = foreground;
	}

	public Color getForeground() {
		return foreground;
	}

	void setBackground(final Color background) {
		this.background = background;
	}

	public Color getBackground() {
		return background;
	}
	
	public void setOption(final Option opt, final boolean val) {
		if(val) 
			options.add(opt);
		else 
			options.remove(opt);
	}

	@Override
	public Style clone(){
		return new Style(getForeground(), getBackground(), options);
	}

	public int getNumber() {
		return number;
	}

	public boolean hasOption(final Option bold) {
		return options.contains(bold);
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (background == null ? 0 : background.hashCode());
		result = PRIME * result + (foreground == null ? 0 : foreground.hashCode());
		result = PRIME * result + (options == null ? 0 : options.hashCode() );
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Style other = (Style) obj;
		if (background == null) {
			if (other.background != null)
				return false;
		} else if (!background.equals(other.background))
			return false;
		if (foreground == null) {
			if (other.foreground != null)
				return false;
		} else if (!foreground.equals(other.foreground))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	public Color getBackgroundForRun(){
		return options.contains(Option.REVERSE) ? foreground : background;
	}

	public Color getForegroundForRun(){
		return options.contains(Option.REVERSE) ? background : foreground;
	}

	public void clearOptions(){
		options.clear();
	}
	
}
