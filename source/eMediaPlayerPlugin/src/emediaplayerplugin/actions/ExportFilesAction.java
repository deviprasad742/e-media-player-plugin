package emediaplayerplugin.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;

import emediaplayerplugin.EMediaPlayerActivator;
import emediaplayerplugin.model.MediaLibrary;
import emediaplayerplugin.ui.EMediaView;

public class ExportFilesAction extends Action {
	private List<File> files2Copy = new ArrayList<File>();
	private MediaLibrary mediaLibrary;
	private static final String DIRECTORY_LOCATION_PREFERENCE = "emediaplayerplugin.actions.ExportFilesAction.directory_location_preference";

	public ExportFilesAction(List<File> selectedFiles, MediaLibrary mediaLibrary) {
		super("Export...");
		this.mediaLibrary = mediaLibrary;
		for (File file : selectedFiles) {
			file = mediaLibrary.getLocalFile(file.getParentFile().getName(), file.getName());
			if (file != null && file.exists()) {
				files2Copy.add(file);
			}
		}
	}

	@Override
	public void run() {
		IPreferenceStore preferenceStore = EMediaPlayerActivator.getDefault().getPreferenceStore();
		String initialLocation = preferenceStore.getString(DIRECTORY_LOCATION_PREFERENCE);
		DirectoryDialog dialog = new DirectoryDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
		dialog.setFilterPath(initialLocation);
		String folderPath = dialog.open();
		if (folderPath != null) {
			preferenceStore.setValue(DIRECTORY_LOCATION_PREFERENCE, folderPath);
			final File root = new File(folderPath);
			Job exportJob = new Job("Exporting Selected Files") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
                    monitor.beginTask("Exporting files to location '" + root.getAbsolutePath()+ ", ", files2Copy.size() + 2);					
					Map<String, Map<File, String>> lib = new HashMap<String, Map<File, String>>();
					monitor.subTask("Loading existing folder strucutre.");
					mediaLibrary.syncRepository(lib, root, true);
					monitor.worked(2);
					try {
						for (File file2Copy : files2Copy) {
							monitor.subTask("Copying file: " +file2Copy.getAbsolutePath());
							if (monitor.isCanceled()) {
								return Status.CANCEL_STATUS;
							}
							mediaLibrary.writeToRepository(root, lib, file2Copy, false, true);
							monitor.worked(1);
						}
					} catch (Exception e) {
						EMediaView.showAndLogError("Export Failed", "Failed to export selected files", e);
					}
					monitor.done();
					return Status.OK_STATUS;
				}
			};
            exportJob.setUser(true);
			exportJob.schedule();

		}
	}

	@Override
	public boolean isEnabled() {
		return !files2Copy.isEmpty();
	}

}
