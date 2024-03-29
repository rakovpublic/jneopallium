package com.rakovpublic.jneuropallium.worker.util;

import java.util.List;

public interface IConfigurationService {
    public List<String> parseClassNames(String json);

    public Boolean loadClassesFromJar(List<String> path, List<String> classNames);

}
