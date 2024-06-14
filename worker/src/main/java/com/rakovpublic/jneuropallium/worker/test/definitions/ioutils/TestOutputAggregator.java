/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TestOutputAggregator implements IOutputAggregator {
    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        ObjectMapper mapper = new ObjectMapper();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("F:\\git\\res\\"+run+"\\"+timestamp+"\\result"));
            writer.write(mapper.writeValueAsString(results));
        } catch (IOException e) {
            e.printStackTrace();
        }finally {

            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
