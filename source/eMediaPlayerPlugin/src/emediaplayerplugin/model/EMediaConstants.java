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
	public static final Image IMAGE_SYNCED_FOLDER = createImage("icons/folder_synced.png");
	public static final Image IMAGE_REMOTE_FOLDER = createImage("icons/folder_remote.png");
	public static final Image IMAGE_REMOTE_FILE = createImage("icons/file_remote.png");
	public static final Image IMAGE_SYNCED_FILE = createImage("icons/file_synced.png");
	public static final Image IMAGE_MUSIC_FILE = createImage("icons/music_file.png");
	public static final Image FAV_MUSIC_FILE = createImage("icons/fav.png");

	public static final String EMEDIA_SHARED_FOLDER= "#eMediaShared";
	public static final String FAV_MEMBER_All= "<All>";
	public static final String FAV_MEMBER_LOCAL= "<Local>";
	public static final String LOCAL_SETTINGS_PATH = "C:\\Program Files\\EMediaPlayerPlugin\\";
	public static final String ALTERNATE_LOCAL_SETTINGS_PATH = "C:\\EMediaPlayerPlugin\\";

	public static final String FAV_MEMBER_UNKNOWN= "<Unknown User>";

	public static final String EXT_JPEG = "jpeg";
	public static final String EXT_JPG = "jpg";


	public static final List<String> SUPPORTED_FORMATS = new ArrayList<String>();

	static{
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
	
}
