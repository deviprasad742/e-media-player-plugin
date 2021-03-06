package emediaplayerplugin.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import emediaplayerplugin.EMediaPlayerActivator;

public class EMediaConstants {

	private static final List<Image> createdImages = new ArrayList<Image>();
	public static final Image IMAGE_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	public static final Image IMAGE_FILE = createImage("icons/file.jpg");
	public static final Image IMAGE_UP = createImage("icons/up.gif");
	public static final Image IMAGE_DOWN = createImage("icons/down.gif");
	public static final Image IMAGE_SYNCED_FOLDER = createImage("icons/folder_synced.png");
	public static final Image IMAGE_REMOTE_FOLDER = createImage("icons/folder_remote.png");
	public static final Image IMAGE_REMOTE_FILE = createImage("icons/file_remote.png");
	public static final Image IMAGE_SYNCED_FILE = createImage("icons/file_synced.png");
	public static final Image IMAGE_MUSIC_FILE = createImage("icons/music_file.png");
	public static final Image IMAGE_PLAY_URL = createImage("icons/play_url.png");
	public static final Image FAV_MUSIC_FILE = createImage("icons/fav.png");
	public static final Image IMAGE_GOOGLE = createImage("icons/google.png");
	public static final Image IMAGE_YOUTUBE= createImage("icons/youtube.png");


	public static final String GOOGLE_URL = "http://www.google.com/";
	public static final String YOUTUBE_URL = "http://www.youtube.com/";

	
	public static final String EMEDIA_SHARED_FOLDER = "#eMediaShared";
	public static final String EMEDIA_SHARED_URL = "[#eMediaSharedUrl]:";

	public static final String UNSHARED_FAVOURITES = "#UnSharedFavourites#";
	public static final String FAV_MEMBER_All = "<All>";
	public static final String FAV_MEMBER_LOCAL = "<Local>";
	public static final String LOCAL_SETTINGS_PATH = "C:\\Program Files\\EMediaPlayerPlugin";
	public static final String ALTERNATE_LOCAL_SETTINGS_PATH = "C:\\EMediaPlayerPlugin";

	public static final String PREFERENCE_NOTIFY = "emedia_plugin_preference_notify";
	public static final String PREFERENCE_SETTINGS_PATH = "emedia_plugin_preference_settings_path";

	public static final String FAV_MEMBER_UNKNOWN = "<Unknown User>";

	public static final String EXT_JPEG = "jpeg";
	public static final String EXT_JPG = "jpg";
	public static final String SEPARATOR = ",";

	public static final List<String> SUPPORTED_FORMATS = new ArrayList<String>();

	static {
		SUPPORTED_FORMATS.add("asf");
		SUPPORTED_FORMATS.add("asx");
		SUPPORTED_FORMATS.add("avi");
		SUPPORTED_FORMATS.add("mp3");
		SUPPORTED_FORMATS.add("mp4");
		SUPPORTED_FORMATS.add("mpeg");
		SUPPORTED_FORMATS.add("wav");
		SUPPORTED_FORMATS.add("wm");
		SUPPORTED_FORMATS.add("wmv");
		SUPPORTED_FORMATS.add("wvx");

		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				Display.getDefault().disposeExec(new Runnable() {
					@Override
					public void run() {
						for (Image image : createdImages) {
							image.dispose();
						}
					}
				});
			}
		});

	}

	private static Image createImage(String filePath) {
		Image image = ImageDescriptor.createFromFile(EMediaPlayerActivator.class, filePath).createImage();
		createdImages.add(image);
		return image;
	}

	public static boolean canNotify() {
		return EMediaPlayerActivator.getDefault().getPreferenceStore().getBoolean(PREFERENCE_NOTIFY);

	}

}
