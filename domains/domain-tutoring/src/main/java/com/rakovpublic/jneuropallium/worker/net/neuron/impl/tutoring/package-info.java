/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Adaptive-tutoring / student cognitive-state modelling use case
 * (<em>docs/use-case-adaptive-tutoring.md</em>).
 *
 * <p>Domain-specific neurons layered on top of the autonomous-AI
 * architecture plus the affect, curiosity and sleep extension modules.
 * Sensing (L0), attention/state (L2), student model (L3), pedagogical
 * planning (L4), selection (L5) and homeostasis/ethics (L7).
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ResponseObserverNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.EngagementSensorNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.AffectObserverNeuron}
 *       — L0 sensing.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.FlowStateNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.FatigueNeuron}
 *       — L2 moment-to-moment state.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ConceptMasteryNeuron}
 *       (BKT) / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.PrerequisiteGraphNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ForgettingCurveNeuron}
 *       (Ebbinghaus + SM-2) — L3 student model.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ZPDPlanningNeuron}
 *       (Vygotsky) / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.HintGenerationNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ScaffoldingNeuron}
 *       — L4 pedagogical planning.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.ContentSelectionNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.PacingNeuron}
 *       — L5 selection.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.WellbeingGuardNeuron}
 *       / {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.FairnessNeuron}
 *       — L7 homeostasis &amp; ethics.</li>
 * </ul>
 *
 * <p>References:
 * <ul>
 *   <li>Corbett, A.T., Anderson, J.R. (1995). Knowledge tracing.
 *       <em>User Modeling and User-Adapted Interaction</em> 4:253–278.</li>
 *   <li>Csikszentmihalyi, M. (1990). <em>Flow: The Psychology of Optimal
 *       Experience.</em> Harper &amp; Row.</li>
 *   <li>Vygotsky, L.S. (1978). <em>Mind in Society.</em> Harvard UP —
 *       zone of proximal development.</li>
 *   <li>Ebbinghaus, H. (1885). <em>Über das Gedächtnis.</em></li>
 *   <li>Wood, D., Bruner, J., Ross, G. (1976). The role of tutoring in
 *       problem solving. <em>J Child Psychol Psychiatry</em> 17(2):89–100.</li>
 *   <li>D'Mello, S., Graesser, A. (2012). AutoTutor and affective AutoTutor.
 *       <em>ACM TiiS</em> 2(4).</li>
 *   <li>Miller, G.A. (1956). The magical number seven, plus or minus two.
 *       <em>Psychological Review</em> 63(2).</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;
