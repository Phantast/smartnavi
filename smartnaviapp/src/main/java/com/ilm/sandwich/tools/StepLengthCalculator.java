package com.ilm.sandwich.tools;

/**
 * Calculates step length from body height input.
 * Supports both metric (cm) and imperial (feet/inches) formats.
 */
public class StepLengthCalculator {

    public static final float STEP_LENGTH_DIVISOR = 222f;

    /**
     * Parses a height string and returns the step length.
     *
     * @param heightInput Body height as string. Metric: "175" (cm).
     *                    Imperial: "5'10", "5'10\"", "5 10", "5ft10in", etc.
     * @return Step length in meters, or -1 if parsing failed.
     */
    public static float calculateStepLength(String heightInput) {
        if (heightInput == null || heightInput.isEmpty()) {
            return -1;
        }
        try {
            if (isImperial(heightInput)) {
                return parseImperial(heightInput);
            } else {
                String cleaned = heightInput.replace(",", ".");
                float heightCm = Float.parseFloat(cleaned);
                return heightCm / STEP_LENGTH_DIVISOR;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Checks whether the input looks like an imperial height value.
     */
    public static boolean isImperial(String input) {
        String lower = input.toLowerCase();
        return input.contains("'") || input.contains("\u2032") || input.contains("\u2019")
                || lower.contains("ft") || lower.contains("foot") || lower.contains("feet");
    }

    private static float parseImperial(String heightInput) {
        // Normalize unicode quotes/primes to ASCII apostrophe
        String normalized = heightInput
                .replace("\u2032", "'")   // prime symbol ′
                .replace("\u2019", "'")   // right single quote '
                .replace("\u2033", "\"")  // double prime ″
                .replace("\u201D", "\""); // right double quote "

        // Remove "ft", "feet", "foot", "in", "inch", "inches" text (case-insensitive)
        // Replace them with the standard separator
        normalized = normalized.replaceAll("(?i)\\s*f(ee|oo)?t\\s*", "'");
        normalized = normalized.replaceAll("(?i)\\s*in(ch(es)?)?\\s*", "");

        // Remove trailing double-quote (from notation like 5'10")
        normalized = normalized.replace("\"", "").trim();

        float feet;
        float inches = 0;

        if (normalized.contains("'")) {
            String[] parts = normalized.split("'", 2);
            feet = Float.parseFloat(parts[0].trim());
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                inches = Float.parseFloat(parts[1].trim());
            }
        } else if (normalized.contains(" ")) {
            // Space-separated: "5 10"
            String[] parts = normalized.trim().split("\\s+", 2);
            feet = Float.parseFloat(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                inches = Float.parseFloat(parts[1]);
            }
        } else {
            // Single number, treat as feet with 0 inches
            feet = Float.parseFloat(normalized.trim());
        }

        float totalInch = 12 * feet + inches;
        return (float) (totalInch * 2.54 / STEP_LENGTH_DIVISOR);
    }
}
