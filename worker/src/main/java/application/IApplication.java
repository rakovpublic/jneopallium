package application;

import synchronizer.IContext;

import java.io.Serializable;

public interface IApplication extends Serializable {
    void startApplication(IContext context);

}
