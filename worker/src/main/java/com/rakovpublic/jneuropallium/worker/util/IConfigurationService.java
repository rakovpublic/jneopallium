package com.rakovpublic.jneuropallium.worker.util;

import java.util.List;

public interface IConfigurationService {
    List<String> parseClassNames(String json);

    Boolean loadClassesFromJar(List<String> path, List<String> classNames);

}
