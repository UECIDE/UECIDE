package edu.mit.blocks.workspace;

public interface ISupportMemento {

    public Object getState();

    public void loadState(Object memento);
}
