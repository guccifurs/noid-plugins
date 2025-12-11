package com.tonic.plugins.lmsnavigator.FightLogic;

/**
 * High-level combat state between us and our LMS target.
 *
 * These correspond to the placeholder fight-logic classes in this package:
 *  - BothFrozen
 *  - BothFrozenMelee
 *  - BothUnfrozen
 *  - TargetFrozenWeUnfrozen
 *  - WeUnfrozenTargetFrozen
 */
public enum CombatState
{
    NO_TARGET,
    BOTH_FROZEN,
    BOTH_FROZEN_MELEE,
    BOTH_UNFROZEN,
    TARGET_FROZEN_WE_UNFROZEN,
    WE_FROZEN_TARGET_UNFROZEN
}
