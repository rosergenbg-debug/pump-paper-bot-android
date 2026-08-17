package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchModePolicyTest {
    @Test fun `research mode is autonomous and master alerts default off`() {
        assertTrue(ResearchModePolicy.ENABLED)
        assertTrue(ResearchModePolicy.AUTONOMOUS_PARTICIPANTS)
        assertTrue(ResearchModePolicy.USE_RESEARCH_APP_BASELINE)
        assertFalse(ResearchModePolicy.DEFAULT_ALERTS_ENABLED)
    }

    @Test fun `master switch gates alerts without disabling research`() {
        assertFalse(ResearchModePolicy.userAlertsAllowed(false))
        assertFalse(ResearchModePolicy.soundAllowed(false))
        assertTrue(ResearchModePolicy.userAlertsAllowed(true))
        assertTrue(ResearchModePolicy.soundAllowed(true))
        assertTrue(ResearchModePolicy.ENABLED)
    }
}
