package emediaplayerplugin.model;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;

public class FavMedia implements IAdaptable {
	private String key;
	private File file;
	private Set<String> members = new HashSet<String>();

	public FavMedia(File file) {
		this.key = getKey(file);
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
		String folderName;
		try {
			folderName = key.substring(0, key.lastIndexOf(File.separator));
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
		return folderName;
	}
	
	public static String getFileName(String key) {
		String fileName;
		try {
			fileName = key.substring(key.lastIndexOf(File.separator) + 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
		return  fileName;
	}
	
	public static String getKey(File file) {
		return file.getParentFile().getName() + File.separator + file.getName();
	}
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter.equals(File.class)) {
			return file;
		}
		return null;
	}
	
	public boolean filterByUser(String filter) {
		for (String member : members) {
			if (member.toLowerCase().contains(filter.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

}
