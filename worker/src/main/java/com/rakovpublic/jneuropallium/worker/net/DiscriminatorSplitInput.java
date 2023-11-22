/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net;

import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;

public interface DiscriminatorSplitInput extends ISplitInput {
    String getDiscriminatorName();
    void setDiscriminatorName(String name);

    DiscriminatorSplitInput getNewInstance();
}
