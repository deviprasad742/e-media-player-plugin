package emediaplayerplugin.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import emediaplayerplugin.model.MediaLibrary;
import emediaplayerplugin.ui.notifications.EventNotifier;
import emediaplayerplugin.ui.notifications.NotificationType;

public class CopyFilesAction extends Action {
	private List<String> files2Copy = new ArrayList<String>();
	public CopyFilesAction(List<File> selectedFiles, MediaLibrary mediaLibrary) {
		super("Copy");
		for (File file : selectedFiles) {
		     file = mediaLibrary.getLocalFile(file.getParentFile().getName(), file.getName());
		     if (file != null && file.exists()) {
		    	 files2Copy.add(file.getAbsolutePath());
		     }
		}
	}
	
	@Override
	public void run() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			Transfer[] transfers = new Transfer[]{FileTransfer.getInstance()};
			Object[] data = new Object[] { files2Copy.toArray(new String[0]) };
			clipboard.setContents(data, transfers);
			String message = "Total '" + files2Copy.size() + " files have been copied to clipboard. Select a folder in windows explorer and use the paste option to copy all the files.";
			EventNotifier.notify("Files Copied", message, NotificationType.CONNECTED);
		} finally {
			clipboard.dispose();
		}
		
	}
	
	@Override
	public boolean isEnabled() {
		return !files2Copy.isEmpty();
	}
	
}
