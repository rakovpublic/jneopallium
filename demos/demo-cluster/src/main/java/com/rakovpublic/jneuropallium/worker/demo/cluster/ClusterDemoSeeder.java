package com.rakovpublic.jneuropallium.worker.demo.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoNeuron;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoPassThroughWeight;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoResultNeuron;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignal;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoSignalChain;
import com.rakovpublic.jneuropallium.worker.util.RedisClientFactory;
import com.rakovpublic.jneuropallium.worker.util.RedisKeys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the cluster demo model into Redis: layers, neurons, the neuron id index, the runtime
 * properties every node reads at start-up, and the input batches.
 * <p>
 * After this has run, the master needs nothing but connection coordinates to schedule the net,
 * and a worker needs nothing but the same coordinates to execute a partition of it.
 *
 * <pre>
 * java ... ClusterDemoSeeder --host 127.0.0.1 --port 6379 --net demo09 \
 *          --layers 3 --neurons 600 --result 50 --epochs 3 \
 *          --master http://127.0.0.1:8080 --threads 2 --flush
 * </pre>
 */
public final class ClusterDemoSeeder {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEMO_ID = "cluster-redis";
    private static final String INPUT_NAME = "clusterDemoInput";
    /** Neuron ids are layer prefixed so that a range in the logs says which layer it belongs to. */
    private static final long LAYER_ID_STRIDE = 1_000_000L;
    private static final long RESULT_ID_BASE = 9_000_000L;

    private ClusterDemoSeeder() {
    }

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        seed(arguments);
    }

    public static void seed(Arguments arguments) {
        try (Jedis jedis = RedisClientFactory.jedis(arguments.host, arguments.port)) {
            if (arguments.flush) {
                deleteNet(jedis, arguments);
            }
            writeProperties(jedis, arguments);
            writeLayers(jedis, arguments);
            writeInput(jedis, arguments);
            System.out.println("Seeded net '" + arguments.net + "' into redis " + arguments.host + ":" + arguments.port);
            for (int layer = 0; layer < arguments.layers; layer++) {
                System.out.println("  layer " + layer + ": " + arguments.neurons + " neurons, ids "
                        + neuronId(layer, 0) + ".." + neuronId(layer, arguments.neurons - 1));
            }
            System.out.println("  result layer " + RedisKeys.RESULT_LAYER_ID + ": " + arguments.result
                    + " neurons, ids " + RESULT_ID_BASE + ".." + (RESULT_ID_BASE + arguments.result - 1));
            System.out.println("  input batches queued: " + arguments.epochs);
        }
    }

    private static void deleteNet(Jedis jedis, Arguments arguments) {
        List<String> keys = new ArrayList<>(jedis.keys(arguments.net + "_*"));
        if (!keys.isEmpty()) {
            jedis.del(keys.toArray(new String[0]));
        }
    }

    private static void writeProperties(Jedis jedis, Arguments arguments) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("master.address", arguments.master);
        properties.put("worker.threads.amount", String.valueOf(arguments.threads));
        properties.put("neuron.pool.size", String.valueOf(arguments.threads));
        properties.put("configuration.demo.id", DEMO_ID);
        jedis.hset(RedisKeys.properties(arguments.net), properties);
    }

    private static void writeLayers(Jedis jedis, Arguments arguments) {
        jedis.del(RedisKeys.layerIds(arguments.net));
        for (int layer = 0; layer < arguments.layers; layer++) {
            jedis.rpush(RedisKeys.layerIds(arguments.net), String.valueOf(layer));
            writeNeurons(jedis, arguments, layer, arguments.neurons, false);
        }
        writeNeurons(jedis, arguments, RedisKeys.RESULT_LAYER_ID, arguments.result, true);
    }

    private static void writeNeurons(Jedis jedis, Arguments arguments, int layer, int count, boolean resultLayer) {
        String neuronsKey = RedisKeys.layerNeurons(arguments.net, layer);
        String indexKey = RedisKeys.layerIndex(arguments.net, layer);
        jedis.del(neuronsKey, indexKey);
        Pipeline pipeline = jedis.pipelined();
        for (int index = 0; index < count; index++) {
            long id = resultLayer ? RESULT_ID_BASE + index : neuronId(layer, index);
            String document = neuronJson(arguments, layer, index, id, resultLayer).toString();
            pipeline.hset(neuronsKey, String.valueOf(id), document);
            pipeline.zadd(indexKey, id, String.valueOf(id));
        }
        pipeline.sync();
    }

    private static long neuronId(int layer, int index) {
        return (layer + 1) * LAYER_ID_STRIDE + index;
    }

    private static ObjectNode neuronJson(Arguments arguments, int layer, int index, long id, boolean resultLayer) {
        String stage = resultLayer ? "result" : "stage-" + layer;
        ObjectNode neuron = MAPPER.createObjectNode();
        neuron.put("neuronId", id);
        neuron.put("currentNeuronClass", resultLayer ? DemoResultNeuron.class.getName() : DemoNeuron.class.getName());
        neuron.put("demoId", DEMO_ID);
        neuron.put("layerRole", stage);
        neuron.put("neuronLabel", stage + "-" + index);
        neuron.put("isProcessed", false);
        neuron.put("changed", false);
        neuron.put("onDelete", false);
        neuron.put("run", -1);

        ObjectNode processorMap = neuron.putObject("processorMap");
        ObjectNode processor = processorMap.putObject(DemoSignal.class.getName());
        processor.put("signalProcessorClass", ClusterSignalProcessor.class.getName());
        processor.put("signalClassName", DemoSignal.class.getName());
        processor.put("stage", stage);
        processor.put("resultStage", resultLayer);

        neuron.set("mergerMap", MAPPER.createObjectNode());
        neuron.set("activationFunctions", MAPPER.createObjectNode());
        neuron.set("axon", axonJson(arguments, layer, index, id, resultLayer));

        ObjectNode chain = neuron.putObject("signalChain");
        chain.put("signalChainClass", DemoSignalChain.class.getName());
        ArrayNode signalClassNames = chain.putArray("signalClassNames");
        signalClassNames.add(DemoSignal.class.getName());
        chain.put("description", "cluster demo layer " + layer + " chain");
        return neuron;
    }

    private static ObjectNode axonJson(Arguments arguments, int layer, int index, long id, boolean resultLayer) {
        ObjectNode axon = MAPPER.createObjectNode();
        ObjectNode connectionMap = axon.putObject("connectionMap");
        ObjectNode addressMap = axon.putObject("addressMap");
        ObjectNode defaultWeights = axon.putObject("defaultWeights");
        axon.put("connectionsWrapped", false);
        ObjectNode defaultWeight = defaultWeights.putObject(DemoSignal.class.getName());
        defaultWeight.put("weightClass", DemoPassThroughWeight.class.getName());
        defaultWeight.put("signalClassName", DemoSignal.class.getName());
        if (resultLayer) {
            return axon;
        }
        boolean lastLayer = layer == arguments.layers - 1;
        int targetLayer = lastLayer ? RedisKeys.RESULT_LAYER_ID : layer + 1;
        int targetSize = lastLayer ? arguments.result : arguments.neurons;
        ArrayNode connections = connectionMap.putArray(DemoSignal.class.getName());
        ObjectNode layerAddress = addressMap.putObject(String.valueOf(targetLayer));
        for (int offset = 0; offset < Math.min(2, targetSize); offset++) {
            int targetIndex = (index + offset) % targetSize;
            long targetNeuronId = lastLayer ? RESULT_ID_BASE + targetIndex : neuronId(targetLayer, targetIndex);
            ObjectNode connection = connections.addObject();
            connection.put("targetLayerId", targetLayer);
            connection.put("sourceLayerId", layer);
            connection.put("targetNeuronId", targetNeuronId);
            connection.put("sourceNeuronId", id);
            ObjectNode weight = connection.putObject("weight");
            weight.put("weightClass", DemoPassThroughWeight.class.getName());
            weight.put("signalClassName", DemoSignal.class.getName());
            connection.put("description", DEMO_ID + " " + layer + "->" + targetLayer);
            layerAddress.putArray(String.valueOf(targetNeuronId));
        }
        return axon;
    }

    /**
     * Queues one batch of input signals per epoch. Batches are plain JSON, so during the demo an
     * operator can inject another one with {@code redis-cli RPUSH}.
     */
    private static void writeInput(Jedis jedis, Arguments arguments) {
        String key = RedisKeys.inputQueue(arguments.net, INPUT_NAME);
        jedis.del(key);
        for (int epoch = 0; epoch < arguments.epochs; epoch++) {
            ArrayNode batch = MAPPER.createArrayNode();
            for (int i = 0; i < arguments.inputSignals; i++) {
                ObjectNode envelope = batch.addObject();
                envelope.put("signalClass", DemoSignal.class.getName());
                ObjectNode signal = envelope.putObject("signal");
                signal.put("demoId", DEMO_ID);
                signal.put("tick", epoch);
                signal.put("entityId", "entity-" + i);
                signal.put("signalType", "ingest");
                signal.put("numericValue", (epoch + 1) * 10.0 + i);
                signal.put("confidence", 1.0);
                signal.put("mode", "ADVISORY");
                signal.put("inputName", INPUT_NAME);
                signal.put("name", INPUT_NAME);
                signal.put("currentClassName", DemoSignal.class.getName());
                signal.put("epoch", epoch);
                signal.put("loop", 0);
                signal.put("timeAlive", 3);
                signal.put("innerLoop", 1);
                signal.put("currentInnerLoop", 0);
            }
            jedis.rpush(key, batch.toString());
        }
    }

    public static String inputName() {
        return INPUT_NAME;
    }

    public static final class Arguments {
        public String host = "127.0.0.1";
        public Integer port = 6379;
        public String net = "demo09";
        public String master = "http://127.0.0.1:8080";
        public int layers = 3;
        public int neurons = 600;
        public int result = 50;
        public int epochs = 3;
        public int inputSignals = 4;
        public int threads = 2;
        public int partitions = 4;
        public boolean flush = false;

        public int partitions() {
            return partitions;
        }

        public static Arguments parse(String[] args) {
            Arguments arguments = new Arguments();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--host" -> arguments.host = args[++i];
                    case "--port" -> arguments.port = Integer.parseInt(args[++i]);
                    case "--net" -> arguments.net = args[++i];
                    case "--master" -> arguments.master = args[++i];
                    case "--layers" -> arguments.layers = Integer.parseInt(args[++i]);
                    case "--neurons" -> arguments.neurons = Integer.parseInt(args[++i]);
                    case "--result" -> arguments.result = Integer.parseInt(args[++i]);
                    case "--epochs" -> arguments.epochs = Integer.parseInt(args[++i]);
                    case "--inputSignals" -> arguments.inputSignals = Integer.parseInt(args[++i]);
                    case "--threads" -> arguments.threads = Integer.parseInt(args[++i]);
                    case "--partitions" -> arguments.partitions = Integer.parseInt(args[++i]);
                    case "--flush" -> arguments.flush = true;
                    default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
                }
            }
            return arguments;
        }
    }
}
