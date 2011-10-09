package emediaplayerplugin.model;

public interface IMediaPlayerListener {
    int EVENT_DEFAULT = 0; 	
	void handleEvent(int eventKind); 
	
}
