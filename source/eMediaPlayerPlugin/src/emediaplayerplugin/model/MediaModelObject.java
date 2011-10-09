package emediaplayerplugin.model;

import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

public class MediaModelObject {
	public static final String NAME = "name";
	
	public Variant getSimpleProperty(OleAutomation auto, String name) {
		Variant varResult = auto.getProperty(property(auto, name));
		if (varResult != null && varResult.getType() != OLE.VT_EMPTY) {
			return varResult;
		}
		return null;
	}

	public OleAutomation getProperty(OleAutomation auto, String name) {
		Variant varResult = auto.getProperty(property(auto, name));
		if (varResult != null && varResult.getType() != OLE.VT_EMPTY) {
			OleAutomation result = null;
			try {
				result = varResult.getAutomation();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(varResult);
			}
			varResult.dispose();
			return result;
		}
		return null;
	}

	public int property(OleAutomation auto, String name) {
		return auto.getIDsOfNames(new String[] { name })[0];
	}

	public Variant invoke(OleAutomation auto, String command, Variant value) {
		return auto.invoke(property(auto, command), new Variant[] { value });
	}

	public Variant invoke(OleAutomation auto, String command, String value) {
		return auto.invoke(property(auto, command),
				new Variant[] { new Variant(value) });
	}

	public Variant invoke(OleAutomation auto, String command) {
		return auto.invoke(property(auto, command));
	}

}
