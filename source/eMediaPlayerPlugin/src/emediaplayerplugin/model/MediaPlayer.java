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
import org.eclipse.swt.widgets.Display;

import emediaplayerplugin.EMediaPlayerActivator;

/**
 * 
 * @author Prasad
 * 
 * Reference: http://msdn.microsoft.com/en-us/library/windows/desktop/dd564034(v=VS.85).aspx
 *
 */

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

	private static final String GET_MODE = "getMode";
	private static final String SET_MODE = "setMode";
	public static final String LOOP = "loop";
	public static final String SHUFFLE = "shuffle";

	public static final String SETTINGS = "settings";
	public static final String VOLUME = "volume";

	private static final String PLAY_LIST_FILE_PATH = EMediaConstants.LOCAL_SETTINGS_PATH + "playlist.japf";

	public OleAutomation oPlayer;
	private MediaControl control;
	private List<MediaFile> playList = new ArrayList<MediaFile>();
	private IListener listener;
	private OleAutomation oSettings;
	private OleAutomation oPlayList;

	public MediaPlayer(OleAutomation oPlayer) {
		this.oPlayer = oPlayer;
		init();
	}

	/**
	 * 
	 */
	private void init() {
		oSettings = getProperty(oPlayer, SETTINGS);
		setProperty(oSettings, VOLUME, new Variant((long)100));
		oPlayList = getProperty(oPlayer, CURRENT_PLAYLIST);
		loadPlaylistSettings();
	}

	private void loadPlaylistSettings() {
		String playList = null;
		File propertiesFile = new File(PLAY_LIST_FILE_PATH);
		if (propertiesFile.exists()) {
			Properties properties = new Properties();
			try {
				properties.load(new FileReader(propertiesFile));
				playList = properties.getProperty(CURRENT_PLAYLIST);
				setRepeat(Boolean.valueOf(properties.getProperty(LOOP)));
				setShuffle(Boolean.valueOf(properties.getProperty(SHUFFLE)));
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
		if (play) {
			final int index = playList.size() -1;
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					playItem(index);
				}
			});
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
			listener.handleEvent(IListener.EVENT_DEFAULT);
		}
	}

	public MediaControl getControl() {
		if (control == null) {
			OleAutomation oControl = getProperty(oPlayer, CONTROLS);
			control = new MediaControl(oControl);
		}
		return control;
	}
	
	public void setListener(IListener listener) {
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
		File propertiesFile = new File(PLAY_LIST_FILE_PATH);
		propertiesFile.getParentFile().mkdirs();
		Properties properties = new Properties();
		properties.put(CURRENT_PLAYLIST, playList);
		properties.put(LOOP,  "" + isRepeat());
		properties.put(SHUFFLE, "" + isShuffle());
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
		oSettings.invoke(property(oSettings, SET_MODE), new Variant[] { new Variant(LOOP), new Variant(repeat)});
	}
	
	public boolean isRepeat() {
		return oSettings.invoke(property(oSettings, GET_MODE), new Variant[] { new Variant(LOOP)}).getBoolean();
	}
 
	public void setShuffle(boolean shuffle) {
		oSettings.invoke(property(oSettings, SET_MODE), new Variant[] { new Variant(SHUFFLE), new Variant(shuffle)});
	}
	
	public boolean isShuffle() {
		return oSettings.invoke(property(oSettings, GET_MODE), new Variant[] { new Variant(SHUFFLE)}).getBoolean();
	}
	
}
