import application.ClusterApplication;
import application.IApplication;
import application.LocalApplication;
import synchronizer.IContext;

public abstract class Runner implements IRunner{
    protected static IContext context;
    public Runner(){
        context=getContext();
    }
    public static void main(String[] args) {
        IApplication application;
        if (args[0].equals( "local")) {
            application = new LocalApplication();
        } else {
            application = new ClusterApplication();
        }
        application.startApplication(context);
    }

}
