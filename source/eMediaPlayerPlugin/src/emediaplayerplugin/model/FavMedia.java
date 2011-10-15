package emediaplayerplugin.model;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FavMedia {
	private String key;
	private File file;
	private Set<String> members = new HashSet<String>();

	public FavMedia(String key, File file) {
		this.key = key;
		this.file = file;
	}

	public String getKey() {
		return key;
	}

	public File getFile() {
		return file;
	}

	public Collection<String> getMembers() {
		return members;
	}
	
	public boolean isValid(){
		return !members.isEmpty();
	}

	public boolean isLocal() {
		return members.contains(EMediaConstants.FAV_MEMBER_LOCAL);
	}
	
	public static String getFolderName(String key) {
		return key.substring(0, key.lastIndexOf(File.separator));
	}
	
	public static String getFileName(String key) {
		return  key.substring(key.lastIndexOf(File.separator) + 1);
	}
	
	public static String getKey(File file) {
		return file.getParentFile().getName() + File.separator + file.getName();
	}

}
