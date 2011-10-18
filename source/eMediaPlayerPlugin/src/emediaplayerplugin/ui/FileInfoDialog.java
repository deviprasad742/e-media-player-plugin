package emediaplayerplugin.ui;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import emediaplayerplugin.model.FavMedia;
import emediaplayerplugin.model.MediaLibrary;

public class FileInfoDialog extends Dialog {
	private MediaLibrary mediaLibrary;
	private FavMedia favMedia;

	public FileInfoDialog(FavMedia favMedia, MediaLibrary mediaLibrary) {
		super(Display.getDefault().getActiveShell());
		this.favMedia = favMedia;
		this.mediaLibrary = mediaLibrary;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("File Info");
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(400, SWT.DEFAULT).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(container);
		File file = favMedia.getFile();
		String fileName = file.getName();
		String folderName = file.getParentFile().getName();
		File localFile = mediaLibrary.getLocalFile(folderName, fileName);
		File remoteFile = mediaLibrary.getRemoteFile(folderName, fileName);
		createRow(container, "File Name: ", fileName);
		createRow(container, "Album: ", folderName);
		if (localFile != null) {
			createRow(container, "Local URL: ", localFile.getAbsolutePath());
		}
		if (remoteFile != null) {
			createRow(container, "Remote URL: ", remoteFile.getAbsolutePath());
		}

		if (favMedia.isValid()) {
			Text favUsers = createRow(container, "Users (+" + favMedia.getMembers().size() + "): ", getFavUsers());
			favUsers.setToolTipText("Users who have added this song to their favourites");
		}
		
		return container;
	}
	
	private String getFavUsers() {
		StringBuilder sb = new StringBuilder();
		for (String user : favMedia.getMembers()) {
			sb.append(user);
			sb.append(", ");
		}
		return sb.substring(0, sb.length() -2);
	}

	private Text createRow(Composite container, String displayName, String value) {
		Label label = new Label(container, SWT.NONE);
		label.setText(displayName);
		GridDataFactory.swtDefaults().applyTo(label);
		final Text text = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		text.setText(value);
		text.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
		return text;
	}

}
