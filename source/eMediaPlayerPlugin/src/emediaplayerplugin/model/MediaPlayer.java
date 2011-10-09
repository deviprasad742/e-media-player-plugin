package emediaplayerplugin.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

import emediaplayerplugin.Activator;

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


	
	private static final String PREF_PLAY_LIST = "emediaplayerplugin.model.MediaPlayer.prefPlayList";

	public OleAutomation oPlayer;
	private MediaControl control;
	private List<MediaFile> playList = new ArrayList<MediaFile>();
	private IMediaPlayerListener listener;

	public MediaPlayer(OleAutomation oPlayer) {
		this.oPlayer = oPlayer;
		init();
	}

	private void init() {
		String playList = Activator.getDefault().getDialogSettings().get(PREF_PLAY_LIST);
		if (playList != null) {
			String[] files = playList.split(",");
			for (String file : files) {
				if (new File(file).exists()) {
					addToPlayList(file, false);
				}
			}
		}
		OleAutomation settingsAut = getProperty(oPlayer, SETTINGS);
		settingsAut.setProperty(property(settingsAut, VOLUME), new Variant[] { new Variant((long)100)});
		settingsAut.invoke(property(settingsAut, "setMode"), new Variant[] { new Variant("loop"), new Variant(true)});
	}

	public List<MediaFile> getPlayList() {
		return playList;
	}

	public void addToPlayList(String fileURL, boolean play) {
		OleAutomation playListAut = getProperty(oPlayer, CURRENT_PLAYLIST);
		Variant media = invoke(oPlayer, NEW_MEDIA, fileURL);
		playList.add(new MediaFile(media));
		invoke(playListAut, ADD_TO_PLAYLIST, media);
		Variant count = getSimpleProperty(playListAut, PLAYLIST_COUNT);
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
		OleAutomation playListAut = getProperty(oPlayer, CURRENT_PLAYLIST);
        invoke(playListAut, REMOVE_ITEM, mediaFile.getMedia());
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
	
	public void savePlaylist() {
		String comma = ",";
		StringBuilder builder = new StringBuilder();
		for (MediaFile mediaFile : playList) {
			builder.append(mediaFile.getUrl());
			builder.append(comma);
		}
		String playList = builder.length() > 0 ? builder.substring(0, builder.length() -1) : builder.toString();
		Activator.getDefault().getDialogSettings().put(PREF_PLAY_LIST, playList);
	}
	
	public void dispose() {
		oPlayer.dispose();
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

}
