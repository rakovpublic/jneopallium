/**
 * Safety-gated {@link com.rakovpublic.jneuropallium.worker.application.IOutputAggregator}
 * implementations that turn neuron decisions into OPC UA writes — never
 * bypassing interlocks, always honouring operator override, always
 * emitting an audit record.
 */
package com.rakovpublic.jneuropallium.worker.output.opcua;
