/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.AlertSeverity;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.VitalType;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.WaveformType;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DiagnosisHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Pure functions converting FHIR (R4 / R5) JSON resources to Jneopallium
 * clinical signals (06-FHIR.md §5 mapping table). Stateless; one
 * {@link PseudonymService} per mapper instance.
 *
 * <p>06-FHIR.md §3 rule 3 + §11 S11 — the mapper:
 * <ul>
 *   <li>Replaces every patient identifier with the pseudonymous id.</li>
 *   <li>Drops free-text {@code Observation.note} / {@code Condition.note}
 *       when {@link FhirBridgeConfig.PrivacyConfig#redactFreeText()} is set
 *       and increments the per-call redaction counter so the bridge log
 *       can record the count without the content.</li>
 * </ul>
 *
 * <p>The mapper is intentionally tolerant of small structural differences
 * between R4 and R5 — it works with the JSON tree directly, which has the
 * same shape for the resources the bridge consumes.
 */
public final class FhirResourceMapper {

    private static final Logger log = LoggerFactory.getLogger(FhirResourceMapper.class);

    /** §5 — vital-signs LOINC → {@link VitalType}. */
    private static final java.util.Map<String, VitalType> LOINC_VITAL =
            java.util.Map.of(
                    "8867-4", VitalType.HR,        // Heart rate
                    "9279-1", VitalType.RESP,      // Respiratory rate
                    "59408-5", VitalType.SPO2,     // SpO2 (peripheral O2 saturation)
                    "2708-6", VitalType.SPO2,      // O2 saturation arterial
                    "8480-6", VitalType.BP_SYS,    // Systolic BP
                    "8462-4", VitalType.BP_DIA,    // Diastolic BP
                    "8310-5", VitalType.TEMP       // Body temperature
            );

    private final PseudonymService pseudonyms;
    private final boolean redactFreeText;
    private int redactionCount;

    public FhirResourceMapper(PseudonymService pseudonyms, boolean redactFreeText) {
        this.pseudonyms = pseudonyms;
        this.redactFreeText = redactFreeText;
    }

    /** Returns and resets the count of free-text fields redacted since last call. */
    public synchronized int drainRedactionCount() {
        int c = redactionCount;
        redactionCount = 0;
        return c;
    }

    /* ===== Bundle helpers ================================================= */

    /** Iterate {@code Bundle.entry[].resource} entries; never returns {@code null}. */
    public List<JsonNode> entries(JsonNode bundle) {
        List<JsonNode> out = new ArrayList<>();
        if (bundle == null || bundle.isMissingNode() || bundle.isNull()) return out;
        JsonNode entries = bundle.get("entry");
        if (entries == null || !entries.isArray()) return out;
        for (Iterator<JsonNode> it = entries.elements(); it.hasNext(); ) {
            JsonNode e = it.next();
            JsonNode r = e == null ? null : e.get("resource");
            if (r != null && !r.isNull()) out.add(r);
        }
        return out;
    }

    /* ===== Resource → signal mappers ===================================== */

    /** {@code Patient} → {@link DemographicSignal}. */
    public DemographicSignal mapPatient(JsonNode patient) {
        if (patient == null || !"Patient".equals(textOrNull(patient.get("resourceType")))) {
            return null;
        }
        String pid = textOrNull(patient.get("id"));
        int age = inferAgeYears(textOrNull(patient.get("birthDate")));
        Sex sex = parseSex(textOrNull(patient.get("gender")));
        return new DemographicSignal(age, sex, List.of(), List.of(),
                pseudonyms.pseudonymise(pid));
    }

    /** {@code Observation} (vital category) → {@link VitalSignal}. */
    public VitalSignal mapVital(JsonNode obs) {
        if (!isObservation(obs)) return null;
        String code = primaryLoinc(obs.get("code"));
        VitalType vt = code == null ? null : LOINC_VITAL.get(code);
        if (vt == null) return null;
        Double value = quantityValue(obs.get("valueQuantity"));
        if (value == null) return null;
        long ts = parseInstantMillis(textOrNull(obs.get("effectiveDateTime")));
        if (ts == 0L) ts = System.currentTimeMillis();
        countRedaction(obs);
        return new VitalSignal(vt, value, ts, pseudonyms.pseudonymise(subjectId(obs)));
    }

    /** {@code Observation} (laboratory) → {@link LabResultSignal}. */
    public LabResultSignal mapLab(JsonNode obs) {
        if (!isObservation(obs)) return null;
        String code = primaryLoinc(obs.get("code"));
        if (code == null) return null;
        // skip vitals — caller routes those through mapVital
        if (LOINC_VITAL.containsKey(code)) return null;
        JsonNode q = obs.get("valueQuantity");
        Double value = quantityValue(q);
        if (value == null) return null;
        String units = q == null ? null : textOrNull(q.get("unit"));
        double[] range = parseReferenceRange(obs.get("referenceRange"));
        long ts = parseInstantMillis(textOrNull(obs.get("effectiveDateTime")));
        if (ts == 0L) ts = System.currentTimeMillis();
        countRedaction(obs);
        return new LabResultSignal(code, value, units, range, ts,
                pseudonyms.pseudonymise(subjectId(obs)));
    }

    /** {@code Observation} (sampled-data) → {@link WaveformSignal}. */
    public WaveformSignal mapWaveform(JsonNode obs) {
        if (!isObservation(obs)) return null;
        JsonNode sd = obs.get("valueSampledData");
        if (sd == null || sd.isNull()) return null;
        double rateHz = 0.0;
        JsonNode period = sd.get("period");
        if (period != null && period.isNumber() && period.asDouble() > 0.0) {
            // FHIR period is the millisecond gap between samples.
            rateHz = 1000.0 / period.asDouble();
        }
        String data = textOrNull(sd.get("data"));
        double[] samples = parseSampledData(data);
        WaveformType type = inferWaveformType(primaryLoinc(obs.get("code")));
        return new WaveformSignal(type, samples, rateHz,
                pseudonyms.pseudonymise(subjectId(obs)));
    }

    /** {@code Condition} → {@link DiagnosisHypothesisSignal} (ingested as confirmed prior). */
    public DiagnosisHypothesisSignal mapCondition(JsonNode cond) {
        if (cond == null || !"Condition".equals(textOrNull(cond.get("resourceType")))) {
            return null;
        }
        String icd10 = primaryIcd10(cond.get("code"));
        if (icd10 == null) return null;
        // Confirmed conditions enter as a strong prior; clinical-status drives confidence.
        String clinicalStatus = primaryCoding(cond.get("clinicalStatus"));
        double posterior = "active".equalsIgnoreCase(clinicalStatus) ? 0.95
                : "recurrence".equalsIgnoreCase(clinicalStatus) ? 0.85
                : "remission".equalsIgnoreCase(clinicalStatus) ? 0.5
                : 0.7;
        countRedaction(cond);
        return new DiagnosisHypothesisSignal(icd10, posterior, List.of(),
                pseudonyms.pseudonymise(subjectId(cond)));
    }

    /** {@code DiagnosticReport} → {@link DiagnosisHypothesisSignal} (lower-confidence). */
    public DiagnosisHypothesisSignal mapDiagnosticReport(JsonNode rep) {
        if (rep == null || !"DiagnosticReport".equals(textOrNull(rep.get("resourceType")))) {
            return null;
        }
        String icd10 = primaryIcd10(rep.get("code"));
        if (icd10 == null) return null;
        return new DiagnosisHypothesisSignal(icd10, 0.6, List.of(),
                pseudonyms.pseudonymise(subjectId(rep)));
    }

    /** {@code MedicationAdministration} → {@link MedicationAdminSignal}. */
    public MedicationAdminSignal mapMedicationAdministration(JsonNode med) {
        if (med == null || !"MedicationAdministration".equals(textOrNull(med.get("resourceType")))) {
            return null;
        }
        String rxNorm = primaryRxNorm(med.get("medicationCodeableConcept"));
        if (rxNorm == null) {
            rxNorm = primaryRxNorm(med.get("medication"));
        }
        if (rxNorm == null) return null;
        JsonNode dosage = med.get("dosage");
        Double dose = null;
        String units = null;
        String route = null;
        if (dosage != null && !dosage.isNull()) {
            JsonNode q = dosage.get("dose");
            dose = quantityValue(q);
            if (q != null) units = textOrNull(q.get("unit"));
            route = primaryCoding(dosage.get("route"));
        }
        long ts = parseInstantMillis(textOrNull(med.get("effectiveDateTime")));
        if (ts == 0L) {
            JsonNode period = med.get("effectivePeriod");
            if (period != null) ts = parseInstantMillis(textOrNull(period.get("start")));
        }
        if (ts == 0L) ts = System.currentTimeMillis();
        return new MedicationAdminSignal(rxNorm,
                dose == null ? 0.0 : dose, units, route, ts,
                pseudonyms.pseudonymise(subjectId(med)));
    }

    /** {@code AllergyIntolerance} → {@link AdverseEventAlertSignal}. */
    public AdverseEventAlertSignal mapAllergyIntolerance(JsonNode allergy) {
        if (allergy == null || !"AllergyIntolerance".equals(textOrNull(allergy.get("resourceType")))) {
            return null;
        }
        String substance = primaryCoding(allergy.get("code"));
        AlertSeverity severity = parseSeverity(textOrNull(allergy.get("criticality")));
        AdverseEventAlertSignal s = new AdverseEventAlertSignal(severity,
                substance == null ? "ALLERGY" : "ALLERGY:" + substance,
                pseudonyms.pseudonymise(subjectId(allergy)));
        return s;
    }

    /**
     * Dispatch a generic FHIR resource to the correct mapper. Returns
     * the mapped signal or {@code null} if the resource is not in the
     * v1 vocabulary.
     */
    public IInputSignal map(JsonNode resource, FhirBridgeConfig.TargetSignal kind) {
        if (resource == null || resource.isNull() || kind == null) return null;
        return switch (kind) {
            case DEMOGRAPHIC -> mapPatient(resource);
            case VITAL -> mapVital(resource);
            case LAB_RESULT -> mapLab(resource);
            case WAVEFORM -> mapWaveform(resource);
            case DIAGNOSIS -> {
                IInputSignal s = mapCondition(resource);
                yield s != null ? s : mapDiagnosticReport(resource);
            }
            case MED_ADMIN -> mapMedicationAdministration(resource);
            case ADVERSE_EVENT -> mapAllergyIntolerance(resource);
        };
    }

    /* ===== shared helpers ================================================ */

    private boolean isObservation(JsonNode obs) {
        return obs != null && "Observation".equals(textOrNull(obs.get("resourceType")));
    }

    private String subjectId(JsonNode resource) {
        JsonNode subject = resource.get("subject");
        if (subject == null || subject.isNull()) {
            JsonNode patient = resource.get("patient");
            if (patient == null || patient.isNull()) return null;
            return referenceToId(textOrNull(patient.get("reference")));
        }
        return referenceToId(textOrNull(subject.get("reference")));
    }

    private static String referenceToId(String reference) {
        if (reference == null) return null;
        int slash = reference.lastIndexOf('/');
        return slash < 0 ? reference : reference.substring(slash + 1);
    }

    /** Pull the LOINC code (system {@code http://loinc.org}) from a CodeableConcept. */
    private static String primaryLoinc(JsonNode codeable) {
        return primaryCodeForSystem(codeable, "http://loinc.org");
    }

    /** Pull the ICD-10 code from a CodeableConcept (CM or PCS). */
    private static String primaryIcd10(JsonNode codeable) {
        String c = primaryCodeForSystem(codeable, "http://hl7.org/fhir/sid/icd-10");
        if (c != null) return c;
        return primaryCodeForSystem(codeable, "http://hl7.org/fhir/sid/icd-10-cm");
    }

    /** Pull the RxNorm code from a CodeableConcept. */
    private static String primaryRxNorm(JsonNode codeable) {
        return primaryCodeForSystem(codeable, "http://www.nlm.nih.gov/research/umls/rxnorm");
    }

    private static String primaryCodeForSystem(JsonNode codeable, String system) {
        if (codeable == null || codeable.isNull()) return null;
        JsonNode coding = codeable.get("coding");
        if (coding == null || !coding.isArray()) return null;
        for (Iterator<JsonNode> it = coding.elements(); it.hasNext(); ) {
            JsonNode c = it.next();
            if (system.equals(textOrNull(c.get("system")))) {
                String code = textOrNull(c.get("code"));
                if (code != null) return code;
            }
        }
        // Fall back to the first coding's code if no exact system match.
        if (coding.size() > 0) return textOrNull(coding.get(0).get("code"));
        return null;
    }

    private static String primaryCoding(JsonNode codeable) {
        if (codeable == null || codeable.isNull()) return null;
        JsonNode coding = codeable.get("coding");
        if (coding != null && coding.isArray() && coding.size() > 0) {
            String c = textOrNull(coding.get(0).get("code"));
            if (c != null) return c;
        }
        // CodeableConcept text fallback.
        return textOrNull(codeable.get("text"));
    }

    private static Double quantityValue(JsonNode q) {
        if (q == null || q.isNull()) return null;
        JsonNode v = q.get("value");
        if (v == null || !v.isNumber()) return null;
        return v.asDouble();
    }

    private static double[] parseReferenceRange(JsonNode range) {
        if (range == null || !range.isArray() || range.size() == 0) return null;
        JsonNode first = range.get(0);
        Double lo = first == null ? null : quantityValue(first.get("low"));
        Double hi = first == null ? null : quantityValue(first.get("high"));
        if (lo == null && hi == null) return null;
        return new double[]{lo == null ? Double.NEGATIVE_INFINITY : lo,
                hi == null ? Double.POSITIVE_INFINITY : hi};
    }

    private static double[] parseSampledData(String data) {
        if (data == null || data.isEmpty()) return new double[0];
        String[] tokens = data.trim().split("\\s+");
        double[] out = new double[tokens.length];
        int n = 0;
        for (String t : tokens) {
            try { out[n++] = Double.parseDouble(t); } catch (NumberFormatException ignored) { /* skip */ }
        }
        if (n == out.length) return out;
        double[] trimmed = new double[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    private static long parseInstantMillis(String iso) {
        if (iso == null || iso.isEmpty()) return 0L;
        try {
            return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) { /* fall through */ }
        try {
            return ZonedDateTime.parse(iso).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            log.debug("Could not parse FHIR instant '{}'", iso);
            return 0L;
        }
    }

    private static int inferAgeYears(String birthDate) {
        if (birthDate == null || birthDate.length() < 4) return 0;
        try {
            int birthYear = Integer.parseInt(birthDate.substring(0, 4));
            int now = ZonedDateTime.now().getYear();
            return Math.max(0, now - birthYear);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Sex parseSex(String gender) {
        if (gender == null) return Sex.UNKNOWN;
        return switch (gender.toLowerCase(Locale.ROOT)) {
            case "male" -> Sex.MALE;
            case "female" -> Sex.FEMALE;
            case "other" -> Sex.INTERSEX;
            default -> Sex.UNKNOWN;
        };
    }

    private static AlertSeverity parseSeverity(String criticality) {
        if (criticality == null) return AlertSeverity.INFO;
        return switch (criticality.toLowerCase(Locale.ROOT)) {
            case "high" -> AlertSeverity.CRITICAL;
            case "low" -> AlertSeverity.WARNING;
            default -> AlertSeverity.INFO;
        };
    }

    private static WaveformType inferWaveformType(String loinc) {
        if (loinc == null) return WaveformType.ECG;
        if (loinc.startsWith("11524")) return WaveformType.ECG;     // 11524-6 = EKG study
        if (loinc.startsWith("8633") || loinc.startsWith("11486")) return WaveformType.PPG;
        if (loinc.startsWith("11459") || loinc.startsWith("EEG"))  return WaveformType.EEG;
        return WaveformType.ECG;
    }

    private static String textOrNull(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    private synchronized void countRedaction(JsonNode resource) {
        if (!redactFreeText || resource == null) return;
        if (resource.has("note") && !resource.get("note").isNull()) redactionCount++;
    }
}
