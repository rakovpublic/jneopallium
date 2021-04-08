package sample;

import com.rakovpublic.jneuropallium.worker.application.Runner;
import com.rakovpublic.jneuropallium.worker.synchronizer.Context;
import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;

public class SimpleRunner extends Runner {
    @Override
    public IContext getContext() {
        return Context.getContext();
    }

}
