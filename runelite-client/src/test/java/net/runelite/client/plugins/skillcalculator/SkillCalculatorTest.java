package net.runelite.client.plugins.skillcalculator;

import com.google.inject.Inject;
import junit.framework.TestCase;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.skillcalculator.beans.SkillData;
import net.runelite.client.plugins.skillcalculator.beans.SkillDataEntry;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.isIn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SkillCalculatorTest {

    @Inject
    SkillCalculator subject;

    @Mock
    private Client client;

    @Mock
    private UICalculatorInputArea uiInput;

    @Mock
    private SpriteManager spriteManager;

    @Mock
    private ItemManager itemManager;

    @Mock
    private JTextField uiFieldCurrentLevel;

    @Mock
    private JTextField uiFieldTargetLevel;

    @Mock
    private CacheSkillData cacheSkillData;

    @Mock
    private SkillData skillData;

    @Mock
    SkillDataEntry skillDataEntry;

    private SkillDataEntry[] skillDataEntries;

    private ArgumentCaptor<ActionListener> actionListenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

    @Before
    public void before()
    {
        when(uiInput.getUiFieldCurrentLevel()).thenReturn(uiFieldCurrentLevel);
        doNothing().when(uiFieldCurrentLevel).addActionListener(actionListenerCaptor.capture());
        when(uiInput.getUiFieldCurrentXP()).thenReturn(Mockito.mock(JTextField.class));
        when(uiInput.getUiFieldTargetLevel()).thenReturn(uiFieldTargetLevel);
        doNothing().when(uiFieldTargetLevel).addActionListener(actionListenerCaptor.capture());
        when(uiInput.getUiFieldTargetXP()).thenReturn(Mockito.mock(JTextField.class));

        subject = new SkillCalculator(client, uiInput, spriteManager, itemManager);
        skillDataEntries = new SkillDataEntry[]{skillDataEntry};
    }

    @Test(timeout=1000)
    public void testSkillCalculator() {
        // Arrange
        when(uiInput.getCurrentLevelInput()).thenReturn(1);
        when(uiInput.getTargetLevelInput()).thenReturn(99);
        try {
            Field f = subject.getClass().getDeclaredField("cacheSkillData");
            f.setAccessible(true);
            f.set(subject, cacheSkillData);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        when(cacheSkillData.getSkillData(CalculatorType.MINING.getDataFile())).thenReturn(skillData);
        when(skillData.getActions()).thenReturn(skillDataEntries);
        when(skillDataEntry.getIcon()).thenReturn(null);
        when(skillDataEntry.getSprite()).thenReturn(null);
        when(skillDataEntry.getXp()).thenReturn(4.5);
        when(skillDataEntry.getLevel()).thenReturn(1);

        // Act
        subject.openCalculator(CalculatorType.MINING);
        actionListenerCaptor.getAllValues().get(0).actionPerformed(null);
        actionListenerCaptor.getAllValues().get(1).actionPerformed(null);

        // Assert
        List<UIActionSlot> uiActionSlots = new ArrayList<>();
        try {
            Field f = subject.getClass().getDeclaredField("uiActionSlots");
            f.setAccessible(true);
            uiActionSlots = (List<UIActionSlot>)f.get(subject);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        JShadowedLabel shadowedLabel = null;
        try {
            Field f = uiActionSlots.get(0).getClass().getDeclaredField("uiLabelActions");
            f.setAccessible(true);
            shadowedLabel = (JShadowedLabel)f.get(uiActionSlots.get(0));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        assertEquals("Lvl. 1 (4.5exp) - 2,896,541 actions", shadowedLabel.getText());
    }

    @Test
    public void testAvailableCalculators() {
        // Arrange
        List<String> skillList = Arrays.asList(
            "Mining", "Agility", "Smithing", "Herblore",
            "Fishing", "Thieving", "Cooking", "Prayer",
            "Crafting", "Firemaking", "Magic", "Fletching",
            "Woodcutting", "Runecraft", "Farming", "Construction",
            "Hunter"
        );

        // Act
        CalculatorType[] calculatorTypes = CalculatorType.values();

        // Assert
        for(CalculatorType calculatorType : calculatorTypes) {
            assertThat(calculatorType.getSkill().getName(), isIn(skillList));
        }
    }

    @Test
    public void testExperienceForLevel() {
        Map<Integer, Integer> levelToExp = new HashMap<>();
        // See https://oldschool.runescape.wiki/w/Experience#Experience_table
        levelToExp.put(1, 0);
        levelToExp.put(2, 83);
        levelToExp.put(10, 1_154);
        levelToExp.put(50, 101_333);
        levelToExp.put(99, 13_034_431);

        for (Map.Entry entry : levelToExp.entrySet()) {
            assertEquals(entry.getValue(), Experience.getXpForLevel((Integer)entry.getKey()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExperienceForLevelZero() {
        Experience.getXpForLevel(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExperienceForLevelTwoHundred() {
        Experience.getXpForLevel(200);
    }

    @Test
    public void testCombatMaxStats() {
        int combatLevel = Experience.getCombatLevel(99, 99, 99, 99, 99, 99, 99);
        assertEquals(126, combatLevel);
    }

    @Test
    public void testCombatNewAccount() {
        int combatLevel = Experience.getCombatLevel(1, 1, 1, 10, 1, 1, 1);
        assertEquals(3, combatLevel);
    }

    @Test
    public void testCombatMyAccount() {
        int combatLevel = Experience.getCombatLevel(70, 73, 70, 71, 78, 56, 63);
        assertEquals(89, combatLevel);
    }
}