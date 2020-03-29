package application;

import synchronizer.IContext;

public interface IRunner {
     IContext getContext();
     void runNet(String mode);


}
