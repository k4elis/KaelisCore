package fr.kaelis.kaeliscore.modules.claims;

/**
 * Roles for claim members
 */
public enum ClaimRole {
    OWNER,      // Full access, can manage claim
    TRUSTED,    // Can build and interact
    MEMBER,     // Can interact only
    NONE        // No access
}
