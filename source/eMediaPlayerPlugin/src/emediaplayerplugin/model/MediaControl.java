package emediaplayerplugin.model;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

public class MediaControl extends MediaModelObject {
	public OleAutomation oControl;
	public static final String PLAY = "play";
	public static final String PLAY_ITEM = "playItem";

	
	public MediaControl(OleAutomation oControl) {
		this.oControl = oControl;
	}
	
	public void play() {
		invoke(oControl, PLAY);
	}
	
	public void playItem(Variant media) {
		invoke(oControl, PLAY_ITEM, media);
	}
	
}
