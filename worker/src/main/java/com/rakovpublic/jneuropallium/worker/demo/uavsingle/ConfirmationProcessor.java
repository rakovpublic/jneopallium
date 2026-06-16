package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.HashSet;
import java.util.Set;

public class ConfirmationProcessor {
    private final Set<String> consumedRequestIds = new HashSet<>();

    public ConfirmationEvaluation evaluate(TargetConfirmationRequestSignal request,
                                           TargetConfirmationResponseSignal response,
                                           long tick) {
        if (request == null) {
            return ConfirmationEvaluation.rejected("NO_PENDING_REQUEST");
        }
        if (response == null || response.getDecision() == ConfirmationDecision.TIMEOUT) {
            if (tick > request.getExpiresAtTick()) {
                return ConfirmationEvaluation.timeout("CONFIRMATION_EXPIRED");
            }
            return ConfirmationEvaluation.pending();
        }
        if (consumedRequestIds.contains(request.getRequestId())) {
            return ConfirmationEvaluation.rejected("DUPLICATE_CONFIRMATION");
        }
        if (tick > request.getExpiresAtTick() || response.getDecision() == ConfirmationDecision.APPROVE_TOO_LATE) {
            consumedRequestIds.add(request.getRequestId());
            return ConfirmationEvaluation.rejected("STALE_CONFIRMATION");
        }
        if (!request.getRequestId().equals(response.getRequestId())
                || response.getDecision() == ConfirmationDecision.WRONG_REQUEST_ID) {
            return ConfirmationEvaluation.rejected("REQUEST_ID_MISMATCH");
        }
        if (!request.getTargetId().equals(response.getTargetId())
                || response.getDecision() == ConfirmationDecision.WRONG_TARGET) {
            consumedRequestIds.add(request.getRequestId());
            return ConfirmationEvaluation.rejected("TARGET_ID_MISMATCH");
        }
        if (!request.getMissionId().equals(response.getMissionId())
                || !request.getUavId().equals(response.getUavId())
                || request.getAction() != response.getAction()) {
            consumedRequestIds.add(request.getRequestId());
            return ConfirmationEvaluation.rejected("CONFIRMATION_BINDING_MISMATCH");
        }
        consumedRequestIds.add(request.getRequestId());
        if (response.getDecision() == ConfirmationDecision.DENY) {
            return ConfirmationEvaluation.denied("DENIED_BY_OPERATOR");
        }
        if (response.getDecision() == ConfirmationDecision.APPROVE) {
            return ConfirmationEvaluation.approved();
        }
        return ConfirmationEvaluation.rejected("UNSUPPORTED_CONFIRMATION_DECISION");
    }
}

