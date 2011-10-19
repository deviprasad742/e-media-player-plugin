package emediaplayerplugin.model;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;

import emediaplayerplugin.EMediaPlayerActivator;
import emediaplayerplugin.ui.EMediaView;

public class FavouritesRepository {
	private static final String MAC_SEPARATOR = ",";
	private static final String FAVOURITES_FILE = "favourites.japf";
	private static final String MEMBERS_FILE = "members.japf";
	private static final String LOCK_FILE = "lock.jalf";
	public static final String SETTINGS_FILE = "settings.jasf";

	private String remoteSettingsPath;
	private Map<String, FavMedia> favMediaMap = Collections.synchronizedMap(new HashMap<String, FavMedia>());
	private String userName;
	private String macAddress;
	private boolean notify = true;

	private Map<String, String> memberNamesMap = Collections.synchronizedMap(new HashMap<String, String>());
	private IListener listener;
	private MediaLibrary mediaLibrary;
	private File membersFile;
	private File remoteFile;
	private File localFile;
	private File settingsFile;
	private String localSettingsPath = EMediaConstants.LOCAL_SETTINGS_PATH;

	public FavouritesRepository(String remoteURL, MediaLibrary mediaLibrary) {
		setRemoteSettingsPath(remoteURL);
		this.mediaLibrary = mediaLibrary;
	    initSettings();
	}

	public void setRemoteSettingsPath(String remoteURL) {
		this.remoteSettingsPath = remoteURL + File.separator + EMediaConstants.EMEDIA_SHARED_FOLDER;
	}

	public void syncRepositories() throws Exception {
		Map<String, FavMedia> favMediaMap = new HashMap<String, FavMedia>();
		Map<String, String> memberNamesMap = new HashMap<String, String>();

		localFile = new File(localSettingsPath, FAVOURITES_FILE);
		remoteFile = new File(remoteSettingsPath, FAVOURITES_FILE);
		membersFile = new File(remoteSettingsPath, MEMBERS_FILE);
	

		if (localFile.exists()) {
			Properties localProperties = new Properties();
			localProperties.load(new FileReader(localFile));
			for (Entry<Object, Object> entry : localProperties.entrySet()) {
				String key = entry.getKey().toString();
				String fileURL = entry.getValue().toString();
				File file = new File(fileURL);
				if (!file.exists()) {
					file = mediaLibrary.getLocalFile(FavMedia.getFolderName(key), FavMedia.getFileName(key));
				}
				if (file != null) {
					FavMedia favMedia = new FavMedia(file);
					favMedia.getMembers().add(EMediaConstants.FAV_MEMBER_LOCAL);
					favMediaMap.put(key, favMedia);
				}
			}
		}

		if (membersFile.exists()) {
			Properties membersMap = new Properties();
			membersMap.load(new FileReader(membersFile));
			for (Entry<Object, Object> entry : membersMap.entrySet()) {
				String macA = entry.getKey().toString();
				String userN = entry.getValue().toString();
				if (macAddress.equals(macA)) {
					userN = userName;
				}
				memberNamesMap.put(macA, userN);
			}
		}

		if (remoteFile.exists()) {
			Properties remoteProperties = new Properties();
			remoteProperties.load(new FileReader(remoteFile));
			for (Entry<Object, Object> entry : remoteProperties.entrySet()) {
				String key = entry.getKey().toString();
				FavMedia favMedia = favMediaMap.get(key);
				if (favMedia == null) {
					File file = mediaLibrary.getFile(FavMedia.getFolderName(key), FavMedia.getFileName(key));
					if (file == null) {
						//create a dummy file
						file = new File(File.separator + EMediaConstants.UNSHARED_FAVOURITES,key);
					}
					if (file != null) {
						favMedia = new FavMedia(file);
						favMediaMap.put(key, favMedia);
					}
				}

				if (favMedia != null) {
					String[] macArray = entry.getValue().toString().split(MAC_SEPARATOR);
					for (String macA : macArray) {
						if (macAddress.equals(macA)) {
							favMedia.getMembers().add(EMediaConstants.FAV_MEMBER_LOCAL);
						} else {
							String member = memberNamesMap.get(macA);
							if (member == null) {
								member = macA;
							}
							favMedia.getMembers().add(member);
						}
					}
				}

			}
		}
		this.favMediaMap.clear();
		this.memberNamesMap.clear();
		this.favMediaMap.putAll(favMediaMap);
		this.memberNamesMap.putAll(memberNamesMap);
		saveAllFavourites();
		notifyListener();
	}

	public void addToFavourites(File file, boolean update) throws Exception {
		String key = FavMedia.getKey(file);
		FavMedia favMedia = favMediaMap.get(key);
		if (favMedia == null) {
			favMedia = new FavMedia(file);
			favMediaMap.put(key, favMedia);
		}
		favMedia.getMembers().add(EMediaConstants.FAV_MEMBER_LOCAL);
		if (update) {
			saveLocalFavourites();
			notifyListener();
		}
	}

	public void removeFromFavourites(File file, boolean update) throws Exception {
		String key = FavMedia.getKey(file);
		FavMedia favMedia = favMediaMap.get(key);
		favMedia.getMembers().remove(EMediaConstants.FAV_MEMBER_LOCAL);
		// donot remove as we have to update remote repo
		//		if (!favMedia.isValid()) {
		//			favMediaMap.remove(key);
		//		}
		if (update) {
			saveLocalFavourites();
			notifyListener();
		}
	}

	public List<FavMedia> getFavMedias() {
		Collection<FavMedia> values = favMediaMap.values();
		ArrayList<FavMedia> filtered = new ArrayList<FavMedia>();
		for (FavMedia favMedia : values) {
			if (favMedia.isValid()) {
				filtered.add(favMedia);
			}
		}
		return filtered;
	}

	public boolean isLocalFavMedia(String fileURL) {
		FavMedia favMedia = getFavMedia(fileURL);
		return favMedia != null && favMedia.isLocal();
	}

	public boolean isFavMedia(String fileURL) {
		FavMedia favMedia = getFavMedia(fileURL);
		return favMedia != null && favMedia.isValid();
	}

	public FavMedia getFavMedia(String fileURL) {
		File file = new File(fileURL);
		FavMedia favMedia = favMediaMap.get(FavMedia.getKey(file));
		return favMedia;
	}

	public void setListener(IListener listener) {
		this.listener = listener;
	}

	private void notifyListener() {
		if (listener != null) {
			listener.handleEvent(IListener.EVENT_DEFAULT);
		}
	}

	public void saveAllFavourites() throws Exception {
		saveLocalFavourites();
		saveRemoteFavourites();
	}

	public void saveLocalFavourites() throws Exception {
		Set<Entry<String, FavMedia>> entrySet = favMediaMap.entrySet();
		Properties properties = new Properties();
		for (Entry<String, FavMedia> entry : entrySet) {
			FavMedia value = entry.getValue();
			if (value.isLocal()) {
				properties.put(entry.getKey(), value.getFile().getAbsolutePath());
			}
		}
		localFile.getParentFile().mkdirs();
		properties.store(new FileWriter(localFile), null);

	}

	public void saveRemoteFavourites() throws Exception {
		FileChannel channel = null;
		try {
			File file = new File(remoteSettingsPath + LOCK_FILE);
			file.getParentFile().mkdirs();
			file.createNewFile();
			channel = new RandomAccessFile(file, "rw").getChannel();
			FileLock lock = channel.lock();
			Properties remoteProperties = new Properties();
			if (remoteFile.exists()) {
				remoteProperties.load(new FileReader(remoteFile));
			}
			for (Entry<String, FavMedia> entry : favMediaMap.entrySet()) {
				String key = entry.getKey();
				FavMedia value = entry.getValue();
				Object object = remoteProperties.get(key);
				if (value.isLocal()) {
					// check and add to files map
					if (object == null) {
						object = macAddress;
					} else {
						boolean remoteExists = false;
						String[] macArray = object.toString().split(MAC_SEPARATOR);
						for (String macA : macArray) {
							if (macAddress.equals(macA)) {
								remoteExists = true;
							}
						}
						if (!remoteExists) {
							object = object.toString() + MAC_SEPARATOR + macAddress;
						}
					}
				} else if (object != null) {
					// remove from files map
					String[] macArray = object.toString().split(MAC_SEPARATOR);
					StringBuilder builder = new StringBuilder();
					for (String macA : macArray) {
						if (!macA.equals(macAddress)) {
							builder.append(macA);
							builder.append(MAC_SEPARATOR);
						}
					}
					if (builder.length() == 0) {
						object = null;
					} else {
						object = builder.substring(0, builder.length() - MAC_SEPARATOR.length()); //remove last separator
					}
				}
				if (object == null) {
					remoteProperties.remove(key);
				} else {
					remoteProperties.put(key, object);
				}
			}
			remoteFile.getParentFile().mkdirs();
			remoteProperties.store(new FileWriter(remoteFile), null);

			Properties members = new Properties();
			if (membersFile.exists()) {
				members.load(new FileReader(membersFile));
			}
			members.put(macAddress, userName);
			members.store(new FileWriter(membersFile), null);

			lock.release();
		} finally {
			if (channel != null) {
				channel.close();
			}
		}
	}

	public List<String> getMembers() {
		return new ArrayList<String>(memberNamesMap.values());
	}

	private void initSettings() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
			byte[] macBytes = ni.getHardwareAddress();
			StringBuilder sb = new StringBuilder();
			for (int k = 0; k < macBytes.length; k++) {
				sb.append(String.format("%02X%s", macBytes[k], (k < macBytes.length - 1) ? "-" : ""));
			}
			macAddress = sb.toString();
		} catch (Exception e) {
			macAddress = "<Unknown>";
		}

		Properties properties = new Properties();
		localSettingsPath = EMediaPlayerActivator.getDefault().getPreferenceStore().getString(EMediaConstants.PREFERENCE_SETTINGS_PATH);

		try {
			userName = InetAddress.getLocalHost().getHostName();
			settingsFile = new File(localSettingsPath, SETTINGS_FILE);
			if (settingsFile.exists()) {
				properties.load(new FileReader(settingsFile));
				Object object = properties.get(EMediaConstants.FAV_MEMBER_LOCAL);
				if (object != null) {
					userName = object.toString();
				}
				String property = properties.getProperty(EMediaConstants.PREFERENCE_NOTIFY);
				if (property != null) {
					notify = Boolean.valueOf(property);
				}
				//update local preferenc from global
			}
		} catch (Exception e) {
			userName = EMediaConstants.FAV_MEMBER_UNKNOWN;
			EMediaView.showAndLogError("Load Settings", null, e);
		}
		EMediaPlayerActivator.getDefault().getPreferenceStore().setValue(EMediaConstants.PREFERENCE_NOTIFY, notify);

	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
		saveSettings();
	}

	private void saveSettings() {
		Properties properties = new Properties();
		settingsFile.getParentFile().mkdirs();
		try {
			IPreferenceStore preferenceStore = EMediaPlayerActivator.getDefault().getPreferenceStore();
			properties.put(EMediaConstants.FAV_MEMBER_LOCAL, userName);
			properties.put(EMediaConstants.PREFERENCE_NOTIFY, "" + preferenceStore.getBoolean(EMediaConstants.PREFERENCE_NOTIFY));
			properties.store(new FileWriter(settingsFile), null);
		} catch (Exception e) {
			EMediaView.showAndLogError("Save Settings", "Failed to save Settings", e);
		}
	}

	
}
