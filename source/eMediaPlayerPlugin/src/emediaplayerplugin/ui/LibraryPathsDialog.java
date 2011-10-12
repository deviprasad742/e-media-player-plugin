package emediaplayerplugin.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import emediaplayerplugin.EMediaPlayerActivator;

public class LibraryPathsDialog extends Dialog {
	private static final String DEFAULT_LOCAL_PATH = "C:\\EMediaMusic\\local";
	private static final String DEFAULT_REMOTE_PATH = "\\\\10.10.19.192\\swdump\\Users\\Prasad\\Music";

	private static final String LOCAL_PATH = "emediaplayerplugin.ui.LibraryPathsDialog.localPath";
	private static final String REMOTE_PATH = "emediaplayerplugin.ui.LibraryPathsDialog.remotePath";

	private String local;
	private String remote;
	private Text localText;
	private Text remoteText;

	public LibraryPathsDialog(String local, String remote) {
		super(Display.getDefault().getActiveShell());
		this.local = local;
		this.remote = remote;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Library Paths");
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(400, SWT.DEFAULT).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);
		localText = createRow(container, "Local: ", local);
		remoteText = createRow(container, "Remote: ", remote);
		return container;
	}

	private Text createRow(Composite container, String displayName, String initValue) {
		Label label = new Label(container, SWT.NONE);
		label.setText(displayName);
		GridDataFactory.swtDefaults().applyTo(label);
		final Text text = new Text(container, SWT.BORDER);
		text.setText(initValue);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
		
		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(Display.getDefault().getActiveShell());
				dialog.setFilterPath(text.getText());
				String path = dialog.open();
				if (path != null) {
					text.setText(path);
				}
			}
		});
		GridDataFactory.swtDefaults().applyTo(browseButton);
		
		return text;
	}

	@Override
	protected void okPressed() {
		local = localText.getText();
		remote = remoteText.getText();
		IDialogSettings dialogSettings = EMediaPlayerActivator.getDefault().getDialogSettings();
		dialogSettings.put(LOCAL_PATH, local);
		dialogSettings.put(REMOTE_PATH, remote);
		super.okPressed();
	}

	public String getLocal() {
		return local;
	}

	public String getRemote() {
		return remote;
	}

	public static String getLocalPath() {
		String path = EMediaPlayerActivator.getDefault().getDialogSettings().get(LOCAL_PATH);
		if (path == null) {
			path = DEFAULT_LOCAL_PATH;
		}
		return path;
	}

	public static String getRemotePath() {
		String path = EMediaPlayerActivator.getDefault().getDialogSettings().get(REMOTE_PATH);
		if (path == null) {
			path = DEFAULT_REMOTE_PATH;
		}
		return path;

	}

}
