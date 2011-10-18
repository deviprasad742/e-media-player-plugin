package emediaplayerplugin.model;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

public class MediaControl extends MediaModelObject {
	public OleAutomation oControl;
	public static final String PLAY = "play";
	public static final String PAUSE = "pause";
	public static final String STOP = "stop";
	public static final String NEXT = "next";
	public static final String PREVIOUS = "previous";

	public static final String PLAY_ITEM = "playItem";
	public static final String CURRENT_ITEM = "currentItem";

	
	public MediaControl(OleAutomation oControl) {
		this.oControl = oControl;
	}
	
	public void play() {
		invoke(oControl, PLAY);
	}
	
	public void pause() {
		invoke(oControl, PAUSE);
	}
	
	public void stop() {
		invoke(oControl, STOP);
	}
	
	public void playNext() {
		invoke(oControl, NEXT);
		play();
	}
	
	public void playPrevious() {
		invoke(oControl, PREVIOUS);
		play();
	}
	
	public void playItem(Variant media) {
		setProperty(oControl, CURRENT_ITEM, media);
		play();
	}
	
	@Override
	public void dispose() {
		oControl.dispose();
	}
	
	
}
