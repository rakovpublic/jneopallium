package com.rakovpublic.jneuropallium.worker.demo.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.application.HttpCommunicationClient;
import com.rakovpublic.jneuropallium.worker.application.HttpRequestResolver;
import com.rakovpublic.jneuropallium.worker.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.worker.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.redis.RedisLayersMeta;
import com.rakovpublic.jneuropallium.worker.net.signals.CycledInputLoadingStrategy;
import com.rakovpublic.jneuropallium.worker.net.signals.OneToAllFirstLayerInputStrategy;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisSignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisSignalStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisSplitInput;

/**
 * Configures a running master for the cluster demo and registers the Redis input.
 * <p>
 * Every storage in the request is a Redis one, so the whole configuration is a handful of
 * connection coordinates - there is no model payload and no file upload. Switching the cluster
 * between file backed and Redis backed storage is this request, not a code change.
 */
public final class ClusterDemoConfigurator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ClusterDemoConfigurator() {
    }

    public static void main(String[] args) throws Exception {
        ClusterDemoSeeder.Arguments arguments = ClusterDemoSeeder.Arguments.parse(args);
        HttpCommunicationClient client = new HttpCommunicationClient();

        String coordinates = redisJson(arguments);
        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest();
        request.setRunOnceIn(0L);
        request.setPartitions(arguments.partitions());
        // Generous on purpose: a partition that takes longer than this is handed to another node
        // while its owner is still working on it, so the timeout has to sit well above the time a
        // single partition takes or the cluster spends its time reprocessing the same range.
        request.setNodeTimeout(180000L);
        request.setDefaultLoopsCount(1);

        request.setLayersMetaClass(RedisLayersMeta.class.getName());
        request.setLayersMetaJson(coordinates);
        request.setSignalsPersistClass(RedisSignalStorage.class.getName());
        request.setSignalsPersistJson(coordinates);
        request.setHistoryClass(RedisSignalHistoryStorage.class.getName());
        request.setHistoryJson(coordinates);
        request.setSplitInputClass(RedisSplitInput.class.getName());
        request.setSplitInputJson(splitInputJson(arguments));
        // No JSON for the loading strategy on purpose: the master then builds it with its no-arg
        // constructor and hands it the layers. Passing JSON routes it through the interface level
        // deserializer, which expects a {"clazz":..,"iInputLoadingStrategy":{..}} envelope.
        request.setInputLoadingStrategyClass(CycledInputLoadingStrategy.class.getName());
        request.setResultRunnerClass(ClusterResultLayerRunner.class.getName());

        client.sendRequest(HttpRequestResolver.createPost(arguments.master + "/configuration/update", request));
        System.out.println("Configuration applied to master " + arguments.master);

        InputRegistrationRequest input = new InputRegistrationRequest();
        input.setiInputSourceClass(RedisInitInput.class.getName());
        input.setiInputSourceJson(initInputJson(arguments));
        input.setInitStrategyClass(OneToAllFirstLayerInputStrategy.class.getName());
        input.setInitStrategy("{}");
        input.setMandatory(false);
        input.setAmountOfRunsToUpdate(1);

        client.sendRequest(HttpRequestResolver.createPost(arguments.master + "/input/register", input));
        System.out.println("Input '" + ClusterDemoSeeder.inputName() + "' registered");
    }

    private static String redisJson(ClusterDemoSeeder.Arguments arguments) throws Exception {
        return MAPPER.writeValueAsString(new Coordinates(arguments.host, arguments.port, arguments.net));
    }

    private static String splitInputJson(ClusterDemoSeeder.Arguments arguments) {
        return "{\"host\":\"" + arguments.host + "\",\"port\":" + arguments.port
                + ",\"neuronNetName\":\"" + arguments.net + "\",\"threads\":" + arguments.threads + "}";
    }

    private static String initInputJson(ClusterDemoSeeder.Arguments arguments) {
        return "{\"host\":\"" + arguments.host + "\",\"port\":" + arguments.port
                + ",\"neuronNetName\":\"" + arguments.net + "\",\"name\":\"" + ClusterDemoSeeder.inputName()
                + "\",\"defaultProcessingFrequency\":{\"epoch\":1,\"loop\":1}}";
    }

    private record Coordinates(String host, Integer port, String neuronNetName) {
    }
}
