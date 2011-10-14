package emediaplayerplugin.model;

public interface IListener {
    int EVENT_DEFAULT = 0; 	
	void handleEvent(int eventKind); 
	
}
