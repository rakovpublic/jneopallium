package synchronizer.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class InstantiationUtils {
    public static <K> K getObject(Class<K> clazz, Object[] args, Class<?>[] params) {
        Constructor<K> ctor = null;
        try {
            K object;
            if(args.length>0){
                ctor = clazz.getDeclaredConstructor(params);
                object = ctor.newInstance(args);
            }else {
                ctor = clazz.getDeclaredConstructor();
                object = ctor.newInstance();
            }
            return object;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            //TODO: add normal logger
        }
        return null;

    }
}
