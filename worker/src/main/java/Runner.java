import application.ClusterApplication;
import application.IApplication;
import application.LocalApplication;
import synchronizer.IContext;

public class Runner {
    public static void main(String [] args){
        IApplication application;
        IContext context =null;
        if(args[0]=="local"){
            application= new LocalApplication();
        }else {
            application= new ClusterApplication();
        }
        application.startApplication(context);

    }
}
