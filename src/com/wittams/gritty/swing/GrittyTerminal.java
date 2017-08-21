package com.wittams.gritty.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import com.wittams.gritty.BackBuffer;
import com.wittams.gritty.Emulator;
import com.wittams.gritty.ScrollBuffer;
import com.wittams.gritty.StyleState;
import com.wittams.gritty.TerminalWriter;
import com.wittams.gritty.Tty;
import com.wittams.gritty.TtyChannel;

public class GrittyTerminal extends JPanel{
	private static final long serialVersionUID = -8213232075937432833L;
	
	private final StyleState styleState;
	private final BackBuffer backBuffer;
	private final ScrollBuffer scrollBuffer;
	private final TermPanel termPanel ;
	private final JScrollBar scrollBar;
	
	private Tty tty;
	private TtyChannel ttyChannel;
	private TerminalWriter terminalWriter;
	private Emulator emulator;
	
	private Thread emuThread;
	
	private AtomicBoolean sessionRunning = new AtomicBoolean();
	private PreConnectHandler preconnectHandler;
	
	public static enum BufferType{
		Back(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getBackBuffer().getLines();
			}
		},
		BackStyle(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getBackBuffer().getStyleLines();
			}
		},
		Damage(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getBackBuffer().getDamageLines();
			}
		},
		Scroll(){ 
			String getValue(GrittyTerminal term ){
				return term.getTermPanel().getScrollBuffer().getLines();
			}
		};
		
		abstract String getValue(GrittyTerminal term );
	}
	
	public GrittyTerminal(){
		super(new BorderLayout());
		
		styleState = new StyleState();
		backBuffer = new BackBuffer(80, 24, styleState);
		scrollBuffer = new ScrollBuffer();
		
		termPanel = new TermPanel(backBuffer, scrollBuffer, styleState);
		terminalWriter = new TerminalWriter(termPanel, backBuffer, styleState);
		preconnectHandler = new PreConnectHandler(terminalWriter);
		termPanel.setKeyHandler(preconnectHandler);
		scrollBar = new JScrollBar();
		
		add(termPanel, BorderLayout.CENTER );
		add(scrollBar, BorderLayout.EAST );
		scrollBar.setModel(termPanel.getBoundedRangeModel() );
		sessionRunning.set(false);
	}
	
	public TermPanel getTermPanel(){
		return termPanel;
	}

	public JScrollBar getScrollBar() {
		return scrollBar;
	}

	public void setTty(Tty tty){
		this.tty = tty;
		ttyChannel = new TtyChannel(tty);
		
		emulator = new Emulator(terminalWriter, ttyChannel);
		this.termPanel.setEmulator(emulator);
	}

	public void start(){
		if(!sessionRunning.get()){
			emuThread = new Thread(new EmulatorTask() );
			emuThread.start();
		}else{
		}
	}
	
	public void stop(){
		if( sessionRunning.get() && emuThread != null )
			emuThread.interrupt();
	}
	
	public boolean isSessionRunning(){
		return sessionRunning.get();
	}
	
	class EmulatorTask implements Runnable{
		public void run(){
			try{
				sessionRunning.set(true);
				Thread.currentThread().setName(tty.getName());
				if(tty.init(preconnectHandler) ){
					Thread.currentThread().setName(tty.getName());
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							termPanel.setKeyHandler(new ConnectedKeyHandler(emulator));
							termPanel.requestFocusInWindow();
						}
					});
					emulator.start();
				}
			}finally{
				try{
					tty.close();
				}catch(Exception e){}
				sessionRunning.set(false);
				termPanel.setKeyHandler(preconnectHandler);
			}
		}		
	}

	public String getBufferText(BufferType type){
		return type.getValue(this);
	}
	
	@Override
	public Dimension getPreferredSize(){
		return new Dimension( termPanel.getPixelWidth() + scrollBar.getPreferredSize().width, termPanel.getPixelHeight());
	}

	public void sendCommand(String string) throws IOException{
		if(sessionRunning.get()){
			ttyChannel.sendBytes(string.getBytes());
		}
	}
	
	@Override
	public boolean requestFocusInWindow() {
		SwingUtilities.invokeLater( new Runnable(){
			public void run() {
				termPanel.requestFocusInWindow();
			}
		});
		return super.requestFocusInWindow();
	}
	
}
