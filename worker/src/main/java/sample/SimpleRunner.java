package sample;

import application.Runner;
import synchronizer.Context;
import synchronizer.IContext;

public class SimpleRunner extends Runner {
    @Override
    public IContext getContext() {
        return Context.getContext();
    }

}
