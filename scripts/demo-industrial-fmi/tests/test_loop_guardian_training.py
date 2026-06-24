from __future__ import annotations

import json
from argparse import Namespace
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import train_loop_guardian_model as trainer  # noqa: E402
import generate_run_reports as reports  # noqa: E402
from run_demo import run_scenario  # noqa: E402


class LoopGuardianTrainingTest(unittest.TestCase):
    def test_reference_training_produces_all_advisory_heads(self):
        examples, _ = trainer.build_examples(10)
        model, metrics = trainer.train_heads(examples)

        self.assertEqual(set(trainer.FINDING_CODES), set(model["heads"].keys()))
        self.assertGreaterEqual(metrics["test"]["macroF1"], 0.85)
        for code in trainer.FINDING_CODES:
            head = model["heads"][code]
            self.assertEqual(len(trainer.FEATURE_NAMES), len(head["weights"]))
            self.assertFalse(head["advisoryTemplate"]["autonomousAction"])

    def test_artifact_export_contains_production_context_and_layers(self):
        with tempfile.TemporaryDirectory() as tmp:
            args = Namespace(
                output_dir=Path(tmp),
                reference_multiplier=10,
                target_corpus_bytes_value=trainer.parse_byte_size("100mb"),
                max_corpus_bytes_value=trainer.parse_byte_size("100mb"),
                split_preview_limit=20,
            )
            seed_examples, _ = trainer.build_examples(1)
            args.effective_reference_multiplier = trainer.reference_multiplier_for_target(
                seed_examples, args.target_corpus_bytes_value, args.max_corpus_bytes_value)
            args.estimated_effective_corpus_bytes = (
                trainer.estimate_corpus_bytes(seed_examples) * args.effective_reference_multiplier)
            args.effective_example_count = len(seed_examples) * args.effective_reference_multiplier
            examples, summaries = trainer.build_examples(args.reference_multiplier)
            model, metrics = trainer.train_heads(examples)

            trainer.write_artifacts(args.output_dir, examples, summaries, model, metrics, args)

            descriptor = json.loads((args.output_dir / "model-descriptor.json").read_text(encoding="utf-8"))
            context = json.loads((args.output_dir / "production-context.json").read_text(encoding="utf-8"))
            layer_2 = json.loads((args.output_dir / "layer-2-maintenance-energy.json").read_text(encoding="utf-8"))
            self.assertEqual("industrial-loop-guardian", descriptor["modelId"])
            self.assertEqual(5, descriptor["totalLayers"])
            self.assertEqual(17, descriptor["totalRealNeurons"])
            self.assertIn("EconomicBasisNeuron", descriptor["networkConfig"]["neuronOwnedLogic"])
            self.assertEqual("false", context["properties"]["configuration.isteacherstudying"])
            self.assertEqual("0", context["properties"]["configuration.discriminatorsAmount"])
            self.assertEqual("true", context["properties"]["configuration.infiniteRun"])
            self.assertEqual("1", context["properties"]["configuration.runoncein"])
            self.assertEqual("diagnosis,economic-basis,safety-envelope,bounded-recommendation",
                             context["properties"]["industrial.neuronOwnedLogic"])
            self.assertIn("configuration.neuronnet.classes", context["properties"])
            first_neuron = layer_2["neurons"][0]
            self.assertIn("currentNeuronClass", first_neuron)
            self.assertIn("resultClasses", first_neuron)
            self.assertIn("signalProcessorClass", first_neuron["processorMap"][next(iter(first_neuron["processorMap"]))])
            self.assertIn("dendrites", first_neuron)
            self.assertIn("signalChain", first_neuron)
            self.assertIn("trainedIndustrialModel", first_neuron)
            self.assertIn("logicalNeuronRole", first_neuron)
            self.assertIn("featureGate", first_neuron)

    def test_demo_run_writes_advisory_jsonl_contract(self):
        with tempfile.TemporaryDirectory() as tmp:
            run_scenario("pump-wear", Path(tmp))
            lines = (Path(tmp) / "pump-wear" / "advisory_findings.jsonl").read_text(encoding="utf-8").splitlines()
            self.assertTrue(lines)
            finding = json.loads(lines[0])
            self.assertIn("asset", finding)
            self.assertIn("confidence", finding)
            self.assertIn("evidence", finding)
            self.assertIn("recommendation", finding)
            self.assertIn("economicBasis", finding)
            self.assertIn("safetyEnvelopeSatisfied", finding)
            self.assertIn("controlBoundary", finding)
            self.assertFalse(finding["autonomousAction"])
            self.assertTrue((Path(tmp) / "pump-wear" / "model_advisory_findings.jsonl").exists())
            self.assertTrue((Path(tmp) / "pump-wear" / "heuristic_advisory_findings.jsonl").exists())

    def test_production_report_contains_advisory_output_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            run_scenario("pump-wear", root)
            report_dir = root / "reports"
            report_dir.mkdir()

            report_path = reports.write_production_report(root, reports.DEFAULT_MODEL_DIR, report_dir)

            advice_evidence = json.loads(
                (report_dir / "production-advisory-output-evidence.json").read_text(encoding="utf-8"))
            manifest = json.loads(
                (report_dir / "production-run-evidence-manifest.json").read_text(encoding="utf-8"))
            report_text = report_path.read_text(encoding="utf-8")

            self.assertEqual(2, advice_evidence["advisoryCount"])
            self.assertIn("advisoryOutputs", manifest)
            self.assertIn("Advisory Output Evidence", report_text)
            self.assertIn("SCHEDULE_PUMP_INSPECTION", report_text)
            self.assertIn("advisory_findings.jsonl", advice_evidence["advisories"][0]["outputRelativePath"])


if __name__ == "__main__":
    unittest.main()
