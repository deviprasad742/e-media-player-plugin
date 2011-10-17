package emediaplayerplugin.ui.notifications;

import org.eclipse.swt.graphics.Color;

public class NotificationColorMapper {

	static Color linkColor = ColorCache.getColor(255, 0, 0);
	static Color commentaryColor = ColorCache.getColor(238, 44, 44);
	
	private static Color greenFG = ColorCache.getColor(240, 255, 240);
	private static Color greenBG = ColorCache.getColor(152, 251, 152);
	private static Color greenBorder = ColorCache.getColor(0, 139, 0);

	private static Color blackFG = ColorCache.getColor(245, 245, 245);
	private static Color blackBG = ColorCache.getColor(175, 175, 175);
	private static Color blackBorder = ColorCache.getColor(0, 0, 0);

	private static Color redFG = ColorCache.getColor(255, 240, 245);
	private static Color redBG = ColorCache.getColor(200, 90, 90);
	private static Color redBorder = ColorCache.getColor(139, 0, 0);

	private static Color orangeFG = ColorCache.getColor(255, 250, 205);;
	private static Color orangeBG = ColorCache.getColor(238, 220, 130);
	private static Color orangeBorder = ColorCache.getColor(139, 54, 15);

	private static Color blueFG = ColorCache.getColor(226, 239, 249);
	private static Color blueBG = ColorCache.getColor(177, 211, 243);
	private static Color blueBorder = ColorCache.getColor(40, 73, 97);
	

	public static Color getForeGround(NotificationType notificationType) {
		if (notificationType == NotificationType.CONNECTED) {
			return greenFG;
		} else if (notificationType == NotificationType.ERROR) {
			return redFG;
		} else if (notificationType == NotificationType.WARN) {
			return orangeFG;
		} else if (notificationType == NotificationType.DISCONNNECTED) {
			return blackFG;
		}
		return blueFG;
	}

	public static Color getBackGround(NotificationType notificationType) {
		if (notificationType == NotificationType.CONNECTED) {
			return greenBG;
		} else if (notificationType == NotificationType.ERROR) {
			return redBG;
		} else if (notificationType == NotificationType.WARN) {
			return orangeBG;
		} else if (notificationType == NotificationType.DISCONNNECTED) {
			return blackBG;
		}
		return blueBG;
	}

	public static Color getBorder(NotificationType notificationType) {
		if (notificationType == NotificationType.CONNECTED) {
			return greenBorder;
		} else if (notificationType == NotificationType.ERROR) {
			return redBorder;
		} else if (notificationType == NotificationType.WARN) {
			return orangeBorder;
		} else if (notificationType == NotificationType.DISCONNNECTED) {
			return blackBorder;
		}
		return blueBorder;
	}

	public static Color getFontColor(NotificationType notificationType) {
		if (notificationType == NotificationType.CONNECTED) {
			return greenBorder;
		} else if (notificationType == NotificationType.ERROR) {
			return redBorder;
		} else if (notificationType == NotificationType.WARN) {
			return orangeBorder;
		} else if (notificationType == NotificationType.DISCONNNECTED) {
			return blackBorder;
		}
		return blueBorder;
	}
}
