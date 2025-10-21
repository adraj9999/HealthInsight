package com.healthinsight;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SymptomEngine
 * - Uses HashMap-based mapping from symptom -> (condition -> weight)
 * - Aggregates scores across user-selected symptoms
 * - Returns top condition suggestions with simple, general care tips
 *
 * Note: Informational only; not diagnostic.
 */
public class SymptomEngine {

    private final Map<String, Map<String, Integer>> symptomConditionWeights = new HashMap<>();
    private final Map<String, String> conditionAdvice = new HashMap<>();
    private final Set<String> urgentSymptoms = new HashSet<>();
    private final List<String> availableSymptoms = new ArrayList<>();

    public SymptomEngine() {
        loadDefaultMappings();
    }

    public List<String> getAvailableSymptoms() {
        return new ArrayList<>(availableSymptoms);
    }

    public EvaluationResult evaluate(List<String> selectedSymptoms, int age, String sex) {
        Map<String, Integer> scores = new HashMap<>();
        boolean urgentFlag = false;

        for (String symptom : selectedSymptoms) {
            if (urgentSymptoms.contains(symptom)) urgentFlag = true;
            Map<String, Integer> weights = symptomConditionWeights.get(symptom);
            if (weights == null) continue;
            for (Map.Entry<String, Integer> e : weights.entrySet()) {
                scores.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }

        // Mild risk heuristics
        if (age >= 65) {
            // slightly bump respiratory conditions in older adults
            bump(scores, "Pneumonia", 1);
            bump(scores, "Influenza (Flu)", 1);
            bump(scores, "COVID-19", 1);
        }

        // Sort by score desc
        List<Map.Entry<String, Integer>> sorted = scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<ConditionSuggestion> top = new ArrayList<>();
        for (int i = 0; i < Math.min(sorted.size(), 3); i++) {
            String cond = sorted.get(i).getKey();
            int score = sorted.get(i).getValue();
            top.add(new ConditionSuggestion(cond, score, conditionAdvice.getOrDefault(cond, "Monitor your symptoms and seek medical advice if needed.")));
        }

        // If nothing matched, provide generic output
        if (top.isEmpty()) {
            top.add(new ConditionSuggestion("No clear match", 0, "Consider rest, fluids, and monitoring. Seek professional advice if symptoms persist or worsen."));
        }

        // Extra urgent hint if specific combinations occur
        if (!urgentFlag) {
            if (selectedSymptoms.contains("Chest Pain/Pressure") && selectedSymptoms.contains("Shortness of Breath")) urgentFlag = true;
            if (selectedSymptoms.contains("High Fever (>=39.5°C)") && age <= 5) urgentFlag = true; // children high fever
        }

        return new EvaluationResult(top, urgentFlag);
    }

    private void bump(Map<String, Integer> scores, String condition, int by) {
        scores.merge(condition, by, Integer::sum);
    }

    private void loadDefaultMappings() {
        // Define symptom list (shown in UI)
        Collections.addAll(availableSymptoms,
                "Fever",
                "High Fever (>=39.5°C)",
                "Chills",
                "Cough",
                "Sore Throat",
                "Runny Nose",
                "Nasal Congestion",
                "Sneezing",
                "Headache",
                "Muscle Aches",
                "Fatigue",
                "Shortness of Breath",
                "Chest Pain/Pressure",
                "Loss of Taste/Smell",
                "Nausea",
                "Vomiting",
                "Diarrhea",
                "Abdominal Pain",
                "Rash/Itchy Skin",
                "Itchy/Watery Eyes",
                "Eye Redness/Irritation",
                "Dizziness/Lightheadedness",
                "Joint Pain",
                "Back Pain",
                "Heart Palpitations",
                "Urinary Burning/Pain",
                "Urinary Frequency/Urgency"
        );

        // Urgent symptom triggers (simplified)
        Collections.addAll(urgentSymptoms,
                "Chest Pain/Pressure",
                "Shortness of Breath",
                "High Fever (>=39.5°C)"
        );

        // Helper: add weights
        weight("Sneezing", "Common Cold", 3);
        weight("Runny Nose", "Common Cold", 3);
        weight("Nasal Congestion", "Common Cold", 3);
        weight("Sore Throat", "Common Cold", 2);
        weight("Cough", "Common Cold", 2);
        weight("Fever", "Common Cold", 1);
        weight("Headache", "Common Cold", 1);
        weight("Fatigue", "Common Cold", 1);

        advice("Common Cold", "Rest, stay hydrated, consider warm fluids. Over-the-counter symptom relief may help. Seek care if symptoms persist/worsen.");

        weight("High Fever (>=39.5°C)", "Influenza (Flu)", 4);
        weight("Chills", "Influenza (Flu)", 3);
        weight("Headache", "Influenza (Flu)", 3);
        weight("Muscle Aches", "Influenza (Flu)", 3);
        weight("Fatigue", "Influenza (Flu)", 2);
        weight("Cough", "Influenza (Flu)", 2);
        weight("Sore Throat", "Influenza (Flu)", 1);
        advice("Influenza (Flu)", "Rest, fluids, and fever control as advised by a clinician. Consider medical care if high risk or severe symptoms.");

        weight("Fever", "COVID-19", 3);
        weight("Cough", "COVID-19", 3);
        weight("Fatigue", "COVID-19", 2);
        weight("Loss of Taste/Smell", "COVID-19", 5);
        weight("Shortness of Breath", "COVID-19", 3);
        weight("Sore Throat", "COVID-19", 2);
        weight("Headache", "COVID-19", 2);
        weight("Muscle Aches", "COVID-19", 2);
        advice("COVID-19", "Consider testing per local guidance. Rest, hydration, and isolation if appropriate. Seek care if breathing issues or high-risk factors.");

        weight("Sneezing", "Allergic Rhinitis (Allergies)", 4);
        weight("Itchy/Watery Eyes", "Allergic Rhinitis (Allergies)", 4);
        weight("Runny Nose", "Allergic Rhinitis (Allergies)", 4);
        weight("Nasal Congestion", "Allergic Rhinitis (Allergies)", 3);
        weight("Sore Throat", "Allergic Rhinitis (Allergies)", 1);
        weight("Eye Redness/Irritation", "Allergic Rhinitis (Allergies)", 2);
        advice("Allergic Rhinitis (Allergies)", "Reduce exposure to triggers, consider saline rinses. Over-the-counter allergy relief may help; consult a pharmacist/clinician.");

        weight("Headache", "Migraine", 5);
        weight("Nausea", "Migraine", 2);
        weight("Vomiting", "Migraine", 1);
        weight("Dizziness/Lightheadedness", "Migraine", 2);
        advice("Migraine", "Rest in a dark, quiet room; stay hydrated. Discuss migraine-specific options with a clinician if recurrent or severe.");

        weight("Nausea", "Gastroenteritis (Stomach Bug)", 3);
        weight("Vomiting", "Gastroenteritis (Stomach Bug)", 4);
        weight("Diarrhea", "Gastroenteritis (Stomach Bug)", 4);
        weight("Abdominal Pain", "Gastroenteritis (Stomach Bug)", 3);
        weight("Fever", "Gastroenteritis (Stomach Bug)", 1);
        advice("Gastroenteritis (Stomach Bug)", "Small sips of fluids and oral rehydration. Seek care for signs of dehydration, blood, or persistent high fever.");

        weight("Nausea", "Foodborne Illness", 4);
        weight("Vomiting", "Foodborne Illness", 5);
        weight("Diarrhea", "Foodborne Illness", 4);
        weight("Abdominal Pain", "Foodborne Illness", 3);
        weight("Fever", "Foodborne Illness", 1);
        advice("Foodborne Illness", "Hydration and gradual diet as tolerated. Seek care if severe pain, blood, or persistent symptoms.");

        weight("Urinary Burning/Pain", "Urinary Tract Irritation/UTI", 5);
        weight("Urinary Frequency/Urgency", "Urinary Tract Irritation/UTI", 4);
        weight("Fever", "Urinary Tract Irritation/UTI", 1);
        weight("Back Pain", "Urinary Tract Irritation/UTI", 1);
        weight("Abdominal Pain", "Urinary Tract Irritation/UTI", 1);
        advice("Urinary Tract Irritation/UTI", "Increase fluids; seek medical evaluation, especially if fever, back pain, or persistent symptoms.");

        weight("Cough", "Acute Bronchitis (Irritated Airways)", 4);
        weight("Fatigue", "Acute Bronchitis (Irritated Airways)", 2);
        weight("Chest Pain/Pressure", "Acute Bronchitis (Irritated Airways)", 2);
        weight("Shortness of Breath", "Acute Bronchitis (Irritated Airways)", 2);
        weight("Fever", "Acute Bronchitis (Irritated Airways)", 1);
        advice("Acute Bronchitis (Irritated Airways)", "Rest, fluids; avoid smoke/irritants. Seek care if high fever, breathing difficulty, or worsening symptoms.");

        weight("Fever", "Pneumonia (Lung Infection)", 3);
        weight("Cough", "Pneumonia (Lung Infection)", 3);
        weight("Shortness of Breath", "Pneumonia (Lung Infection)", 4);
        weight("Chest Pain/Pressure", "Pneumonia (Lung Infection)", 3);
        weight("Chills", "Pneumonia (Lung Infection)", 2);
        weight("Fatigue", "Pneumonia (Lung Infection)", 1);
        advice("Pneumonia (Lung Infection)", "May require clinical evaluation. Seek care promptly, especially with breathing issues or high fever.");

        weight("Sore Throat", "Sore Throat (Strep/Other)", 5);
        weight("Fever", "Sore Throat (Strep/Other)", 2);
        weight("Headache", "Sore Throat (Strep/Other)", 1);
        advice("Sore Throat (Strep/Other)", "Hydration, throat soothing measures. Consider medical check, especially with fever or severe pain.");

        weight("Nasal Congestion", "Sinus Irritation (Sinusitis)", 4);
        weight("Runny Nose", "Sinus Irritation (Sinusitis)", 3);
        weight("Headache", "Sinus Irritation (Sinusitis)", 3);
        weight("Sore Throat", "Sinus Irritation (Sinusitis)", 1);
        weight("Cough", "Sinus Irritation (Sinusitis)", 1);
        advice("Sinus Irritation (Sinusitis)", "Steam, saline rinses, hydration. Seek care if symptoms are severe or persist.");

        weight("Rash/Itchy Skin", "Skin Irritation (Dermatitis)", 5);
        weight("Itchy/Watery Eyes", "Skin Irritation (Dermatitis)", 1);
        advice("Skin Irritation (Dermatitis)", "Avoid irritants; gentle skincare. Seek medical advice if widespread, painful, or infected.");

        weight("Dizziness/Lightheadedness", "Dehydration/Low Fluids", 3);
        weight("Fatigue", "Dehydration/Low Fluids", 2);
        weight("Headache", "Dehydration/Low Fluids", 2);
        weight("Vomiting", "Dehydration/Low Fluids", 1);
        weight("Diarrhea", "Dehydration/Low Fluids", 1);
        advice("Dehydration/Low Fluids", "Increase fluid intake (oral rehydration). Seek care for confusion, fainting, or inability to keep fluids down.");

        weight("Chest Pain/Pressure", "Reflux/Irritation (GERD-like)", 1);
        weight("Abdominal Pain", "Reflux/Irritation (GERD-like)", 2);
        weight("Nausea", "Reflux/Irritation (GERD-like)", 1);
        advice("Reflux/Irritation (GERD-like)", "Smaller meals, avoid trigger foods, avoid lying down after eating. Seek evaluation for severe or persistent pain.");

        weight("Heart Palpitations", "Stress/Anxiety Symptoms", 4);
        weight("Shortness of Breath", "Stress/Anxiety Symptoms", 3);
        weight("Dizziness/Lightheadedness", "Stress/Anxiety Symptoms", 2);
        weight("Chest Pain/Pressure", "Stress/Anxiety Symptoms", 2);
        advice("Stress/Anxiety Symptoms", "Breathing and grounding techniques may help. Seek medical evaluation to rule out other causes, especially with chest pain.");

    }

    private void weight(String symptom, String condition, int w) {
        symptomConditionWeights.computeIfAbsent(symptom, k -> new HashMap<>())
                .merge(condition, w, Integer::sum);
    }

    private void advice(String condition, String tip) {
        conditionAdvice.put(condition, tip);
    }

    /* ------------ Result Models ------------- */

    public record ConditionSuggestion(String conditionName, int score, String advice) {}

    public static class EvaluationResult {
        private final List<ConditionSuggestion> topSuggestions;
        private final boolean urgent;

        public EvaluationResult(List<ConditionSuggestion> topSuggestions, boolean urgent) {
            this.topSuggestions = topSuggestions;
            this.urgent = urgent;
        }

        public List<ConditionSuggestion> getTopSuggestions() {
            return topSuggestions;
        }

        public boolean isUrgent() {
            return urgent;
        }
    }
}