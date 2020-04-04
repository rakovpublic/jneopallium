package synchronizer.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class InstantiationUtils {
    public static <K> K getObject(Class<K> clazz, Object[] args, List<Class<?>> params) {
        Constructor<K> ctor = null;
        try {
            K object;
            if(args.length>0){
                Class<?>[] array= new Class<?>[params.size()];
                int i=0;
                for(Class<?> cl:params){
                    array[i]=cl;
                    i++;
                }
                ctor = clazz.getDeclaredConstructor(array);
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
