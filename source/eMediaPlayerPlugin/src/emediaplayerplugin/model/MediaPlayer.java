package emediaplayerplugin.model;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

import emediaplayerplugin.EMediaPlayerActivator;

public class MediaPlayer extends MediaModelObject {

	public static final String CURRENT_PLAYLIST = "currentPlaylist";
	public static final String CONTROLS = "controls";
	public static final String PLAYLIST_COUNT = "count";
	public static final String ADD_TO_PLAYLIST = "appendItem";
	public static final String NEW_MEDIA = "newMedia";
	public static final String CURRENT_MEDIA = "currentMedia";
	public static final String ITEM = "item";
	public static final String URL = "URL";
	public static final String REMOVE_ITEM = "removeItem";
	
	public static final String SETTINGS = "settings";
	public static final String VOLUME = "volume";


	private static final String PLAY_LIST_FILE = "C:\\Program Files\\EMediaPlayerPlugin\\playlist.japf";

	public OleAutomation oPlayer;
	private MediaControl control;
	private List<MediaFile> playList = new ArrayList<MediaFile>();
	private IMediaPlayerListener listener;
	private OleAutomation oSettings;
	private OleAutomation oPlayList;

	public MediaPlayer(OleAutomation oPlayer) {
		this.oPlayer = oPlayer;
		init();
	}

	private void init() {
		oSettings = getProperty(oPlayer, SETTINGS);
		setProperty(oSettings, VOLUME, new Variant((long)100));
		setRepeat(true);
		oPlayList = getProperty(oPlayer, CURRENT_PLAYLIST);
		loadPlaylist();
	}

	private void loadPlaylist() {
		String playList = null;
		File propertiesFile = new File(PLAY_LIST_FILE);
		if (propertiesFile.exists()) {
			Properties properties = new Properties();
			try {
				properties.load(new FileReader(propertiesFile));
				playList = properties.getProperty(CURRENT_PLAYLIST);
			} catch (Exception e) {
				EMediaPlayerActivator.getDefault().logException(e);
			}
		}
		if (playList != null) {
			String[] files = playList.split(",");
			for (String file : files) {
				if (new File(file).exists()) {
					addToPlayList(file, false);
				}
			}
		}
	}

	public List<MediaFile> getPlayList() {
		return playList;
	}

	public void addToPlayList(String fileURL, boolean play) {
		Variant media = invoke(oPlayer, NEW_MEDIA, fileURL);
		playList.add(new MediaFile(media));
		invoke(oPlayList, ADD_TO_PLAYLIST, media);
		Variant count = getSimpleProperty(oPlayList, PLAYLIST_COUNT);
		if (play && count.getInt() == 1) {
			getControl().play();
		}
        notifyListener();
	}
	
	public void playItem(int index) {
		MediaFile media = playList.get(index);
		getControl().playItem(media.getMedia());
	}
 	
	public void removeItem(int index) {
	    MediaFile mediaFile = playList.remove(index);
        invoke(oPlayList, REMOVE_ITEM, mediaFile.getMedia());
        notifyListener();
	}
	

	private void notifyListener() {
		if (listener != null) {
			listener.handleEvent(IMediaPlayerListener.EVENT_DEFAULT);
		}
	}

	public MediaControl getControl() {
		if (control == null) {
			OleAutomation oControl = getProperty(oPlayer, CONTROLS);
			control = new MediaControl(oControl);
		}
		return control;
	}
	
	public void setListener(IMediaPlayerListener listener) {
		this.listener = listener;
	}
	
	public void savePlaylist() throws IOException {
		String comma = ",";
		StringBuilder builder = new StringBuilder();
		for (MediaFile mediaFile : playList) {
			builder.append(mediaFile.getUrl());
			builder.append(comma);
		}
		String playList = builder.length() > 0 ? builder.substring(0, builder.length() -1) : builder.toString();
		File propertiesFile = new File(PLAY_LIST_FILE);
		propertiesFile.getParentFile().mkdirs();
		Properties properties = new Properties();
		properties.put(CURRENT_PLAYLIST, playList);
		properties.store(new FileWriter(propertiesFile), null);
	}
	
	public void dispose() {
		oPlayer.dispose();
		oPlayList.dispose();
		oSettings.dispose();
	}
	
	private static final int PLAYING = 3;
	
	public void toggleState() {
         Variant state = getSimpleProperty(oPlayer, "playState");		
         if (state.getInt() == PLAYING) {
        	 getControl().pause();
         } else {
        	 getControl().play();
         }
	}
	
	public void setRepeat(boolean repeat) {
		oSettings.invoke(property(oSettings, "setMode"), new Variant[] { new Variant("loop"), new Variant(true)});
	}
 
}
