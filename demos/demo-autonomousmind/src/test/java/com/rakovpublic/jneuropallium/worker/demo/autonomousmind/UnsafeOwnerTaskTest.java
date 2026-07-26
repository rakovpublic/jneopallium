package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsafeOwnerTaskTest {
    @Test
    void rejectsForbiddenOwnerRequestBeforeExecution() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("unsafe_owner_task");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "unsafeRejected"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "ownerDoesNotOverrideSafety"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "taskNotCompleted"));
    }
}
