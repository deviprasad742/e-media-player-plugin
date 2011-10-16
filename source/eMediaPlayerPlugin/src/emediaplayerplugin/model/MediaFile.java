package emediaplayerplugin.model;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

public class MediaFile extends MediaModelObject implements IAdaptable {
	public static final String SOURCE_URL = "sourceURL";
	public static final String DURATION = "durationString";
	
	public OleAutomation oMedia;
	private Variant media;
	private String name;
	private String url;
	private String duration;
	
	public MediaFile(Variant media) {
		this.media = media;
		this.oMedia = media.getAutomation();
	}
	
	public Variant getMedia() {
		return media;
	}
	
	public String getName() {
		if (name == null) {
			name = getSimpleProperty(oMedia, NAME).getString();
		}
		return name;
	}
	
	public String getUrl() {
		if (url == null) {
			url = getSimpleProperty(oMedia, SOURCE_URL).getString();
		}
		return url;
	}
	
	public String getDuration() {
		if (duration == null) {
			duration = getSimpleProperty(oMedia, DURATION).getString();
		}
		return duration;
	}
	
	public boolean isWebUrl(){
		return MediaLibrary.isWebUrl(getUrl());
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter.equals(File.class)) {
			return new File(url);
		}
		return null;
	}
	
}
