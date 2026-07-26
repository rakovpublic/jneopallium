package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class ConfirmationEvaluation {
    public final ConfirmationStatus status;
    public final String reason;

    private ConfirmationEvaluation(ConfirmationStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public static ConfirmationEvaluation approved() {
        return new ConfirmationEvaluation(ConfirmationStatus.APPROVED, "CONFIRMATION_APPROVED");
    }

    public static ConfirmationEvaluation denied(String reason) {
        return new ConfirmationEvaluation(ConfirmationStatus.DENIED, reason);
    }

    public static ConfirmationEvaluation timeout(String reason) {
        return new ConfirmationEvaluation(ConfirmationStatus.TIMEOUT, reason);
    }

    public static ConfirmationEvaluation rejected(String reason) {
        return new ConfirmationEvaluation(ConfirmationStatus.REJECTED, reason);
    }

    public static ConfirmationEvaluation pending() {
        return new ConfirmationEvaluation(ConfirmationStatus.PENDING, "WAITING_FOR_CONFIRMATION");
    }
}

