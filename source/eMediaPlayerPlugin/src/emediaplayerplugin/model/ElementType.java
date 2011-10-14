package emediaplayerplugin.model;

import org.eclipse.swt.graphics.Image;

public enum ElementType {
	FILE_NORMAL, FILE_REMOTE, FILE_SYNCED, FOLDER_NORMAL, FOLDER_REMOTE, FOLDER_SYNCED;

	public Image getImage() {
		switch (this) {
		case FILE_NORMAL:
			return EMediaConstants.IMAGE_FILE;
		case FILE_REMOTE:
			return EMediaConstants.IMAGE_REMOTE_FILE;
		case FILE_SYNCED:
			return EMediaConstants.IMAGE_SYNCED_FILE;
		case FOLDER_NORMAL:
			return EMediaConstants.IMAGE_FOLDER;
		case FOLDER_REMOTE:
			return EMediaConstants.IMAGE_REMOTE_FOLDER;
		default:
			return EMediaConstants.IMAGE_SYNCED_FOLDER;
		}
	}
	
}
