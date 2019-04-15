package application;

import net.storages.IStructMeta;
import synchronizer.IContext;

public interface IApplication {
    void startApplication(IContext context, IStructMeta meta);
}
