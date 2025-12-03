package com.tonic.plugins.lmsnavigator;

import com.tonic.plugins.lmsnavigator.FightLogic.FreezeManager;
import com.tonic.plugins.lmsnavigator.FightLogic.AttackTimers;
import com.tonic.plugins.lmsnavigator.FightLogic.FightStateManager;
import com.tonic.plugins.lmsnavigator.FightLogic.CombatState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class LMSOverlayFight extends Overlay
{
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public LMSOverlayFight()
    {
        // Place fight overlay at bottom-left to avoid overlapping the status overlay
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(170, 130));

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("LMS Fight")
            .build());

        // Freeze ticks
        int playerFreeze = FreezeManager.getPlayerFreezeTicks();
        int targetFreeze = FreezeManager.getTargetFreezeTicks();

        String frozenText = playerFreeze > 0 ? (playerFreeze + " ticks") : "No";
        String targetFrozenText = targetFreeze > 0 ? (targetFreeze + " ticks") : "No";

        addLine("Frozen:", frozenText);
        addLine("Target Frozen:", targetFrozenText);

        // Combat state based on freeze + distance
        CombatState state = FightStateManager.getCurrentState();
        String combatStateText;
        switch (state)
        {
            case BOTH_FROZEN_MELEE:
                combatStateText = "Both frozen (melee)";
                break;
            case BOTH_FROZEN:
                combatStateText = "Both frozen";
                break;
            case BOTH_UNFROZEN:
                combatStateText = "Both unfrozen";
                break;
            case TARGET_FROZEN_WE_UNFROZEN:
                combatStateText = "Target frozen, we unfrozen";
                break;
            case WE_FROZEN_TARGET_UNFROZEN:
                combatStateText = "We frozen, target unfrozen";
                break;
            case NO_TARGET:
            default:
                combatStateText = TargetManagement.isInCombat() ? "In combat" : "Idle";
                break;
        }
        addLine("Combat state:", combatStateText);

        // Attack cooldowns
        int playerCd = AttackTimers.getPlayerCooldown();
        int targetCd = AttackTimers.getTargetCooldown();

        String cdText = playerCd > 0 ? (playerCd + " ticks") : "Ready";
        String targetCdText = targetCd > 0 ? (targetCd + " ticks") : "Ready";

        addLine("CD:", cdText);
        addLine("Target CD:", targetCdText);

        // Debug: show the HP value LMS Navigator is using for shark logic
        int debugHp = AttackTimers.getPlayerHpFromOrb();
        addLine("HP (LMSNav):", String.valueOf(debugHp));

        // Placeholders for future fight logic
        addLine("Damage Target:", "0");
        addLine("Damage:", "0");
        addLine("Spec Target:", "0%");
        addLine("Spec:", "0%");

        return panelComponent.render(graphics);
    }

    private void addLine(String label, String value)
    {
        String line = label + " " + value;
        panelComponent.getChildren().add(TitleComponent.builder()
            .text(line)
            .build());
    }
}
