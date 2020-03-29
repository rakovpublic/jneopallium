package application;

import application.ClusterApplication;
import application.IApplication;
import application.LocalApplication;
import synchronizer.IContext;

public abstract class Runner implements IRunner {
    private  IContext context;
    public Runner(){
        context=getContext();
    }

    @Override
    public void runNet(String mode) {
        IApplication application;
        if (mode.equals( "local")) {
            application = new LocalApplication();
        } else {
            application = new ClusterApplication();
        }
        application.startApplication(context);
    }
}
