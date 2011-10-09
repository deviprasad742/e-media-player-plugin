package emediaplayerplugin.actions;

import org.eclipse.jface.action.IAction;

import emediaplayerplugin.ui.EMediaView;

public class ShowEMediaViewAction extends ToolBarAction {

	public void run(IAction action) {
		EMediaView.showView();
	}

}
