package emediaplayerplugin.actions;
import org.eclipse.jface.action.IAction;

import emediaplayerplugin.ui.EMediaView;


public class PlayPreviousAction extends ToolBarAction {

	public void run(IAction action) {
		EMediaView mediaView = EMediaView.getMediaView();
		if (mediaView == null) {
			mediaView = EMediaView.showView();
		}
		mediaView.getMediaPlayer().getControl().playPrevious();
	}


}
