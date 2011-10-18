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

public class EMediaPreferencesDialog extends Dialog {
	private static final String DEFAULT_LOCAL_PATH = "C:\\EMediaPlayerPlugin\\local";
	private static final String DEFAULT_REMOTE_PATH = "C:\\EMediaPlayerPlugin\\remote";

	private static final String LOCAL_PATH = "emediaplayerplugin.ui.LibraryPathsDialog.localPath";
	private static final String REMOTE_PATH = "emediaplayerplugin.ui.LibraryPathsDialog.remotePath";

	private String local;
	private String remote;
	private String userName;
	private Text localText;
	private Text remoteText;
	private Text userNameText;

	public EMediaPreferencesDialog(String local, String remote, String userName) {
		super(Display.getDefault().getActiveShell());
		this.local = local;
		this.remote = remote;
		this.userName = userName;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Preferences");
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(400, SWT.DEFAULT).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);
		localText = createRow(container, "Local: ", local);
		remoteText = createRow(container, "Remote: ", remote);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("User: ");
		GridDataFactory.swtDefaults().applyTo(label);
		userNameText = new Text(container, SWT.BORDER);
		userNameText.setText(userName);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(userNameText);
		
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
		userName = userNameText.getText();
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
	
	public String getUserName() {
		return userName;
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
