package cs451.communication;

public interface TopLevelBroadcast {
    public void broadcast();
    public void urbDeliver(TopLevelMessage msg);
}
