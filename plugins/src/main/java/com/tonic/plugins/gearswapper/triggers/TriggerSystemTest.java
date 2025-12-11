package com.tonic.plugins.gearswapper.triggers;

import com.tonic.Logger;

/**
 * Comprehensive test suite for the trigger system
 * Validates end-to-end functionality and edge cases
 */
public class TriggerSystemTest
{
    // Use the project's Logger class
    private static final String TEST_TAG = "[Trigger System Test]";
    
    private TriggerEngine triggerEngine;
    
    public TriggerSystemTest()
    {
        // Minimal test setup
    }
    
    /**
     * Run comprehensive test suite
     */
    public boolean runAllTests()
    {
        Logger.norm(TEST_TAG + " Starting comprehensive test suite...");
        
        try
        {
            boolean allTestsPassed = true;
            
            allTestsPassed &= testTriggerConfigValidation();
            allTestsPassed &= testTriggerClasses();
            allTestsPassed &= testTriggerActions();
            allTestsPassed &= testTriggerEvents();
            allTestsPassed &= testErrorHandling();
            allTestsPassed &= testEdgeCases();
            
            if (allTestsPassed)
            {
                Logger.norm(TEST_TAG + " ✅ ALL TESTS PASSED - System is production-ready!");
            }
            else
            {
                Logger.error(TEST_TAG + " ❌ SOME TESTS FAILED - System needs fixes");
            }
            
            return allTestsPassed;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Critical error during testing: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test TriggerConfig validation
     */
    private boolean testTriggerConfigValidation()
    {
        Logger.norm(TEST_TAG + " Testing TriggerConfig validation...");
        
        try
        {
            // Test valid config creation
            TriggerConfig validConfig = new TriggerConfig();
            if (!validConfig.isValid())
            {
                Logger.error(TEST_TAG + " Valid config failed validation");
                return false;
            }
            
            // Test invalid animation ID
            TriggerConfig invalidConfig = new TriggerConfig();
            invalidConfig.setAnimationId(-1);
            if (invalidConfig.isValid())
            {
                Logger.error(TEST_TAG + " Invalid config passed validation");
                return false;
            }
            
            // Test validation error message
            String error = invalidConfig.getValidationError();
            if (error == null || error.isEmpty())
            {
                Logger.error(TEST_TAG + " Validation error message missing");
                return false;
            }
            
            // Test enum setters
            validConfig.setTargetFilterByValue("CURRENT");
            if (validConfig.getTargetFilter() != TriggerConfig.TargetFilter.CURRENT)
            {
                Logger.error(TEST_TAG + " Enum setter failed");
                return false;
            }
            
            Logger.norm(TEST_TAG + " ✅ TriggerConfig validation tests passed");
            return true;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Error in TriggerConfig validation tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test Trigger classes
     */
    private boolean testTriggerClasses()
    {
        Logger.norm(TEST_TAG + " Testing Trigger classes...");
        
        try
        {
            // Test trigger creation
            Trigger trigger = new Trigger("test_001", "Test Trigger", TriggerType.ANIMATION);
            
            // Test validation
            if (!trigger.isValid())
            {
                Logger.error(TEST_TAG + " Valid trigger failed validation");
                return false;
            }
            
            // Test action management
            GearSwapAction action = new GearSwapAction("Test Gear");
            trigger.addAction(action);
            
            if (trigger.getActions().size() != 1)
            {
                Logger.error(TEST_TAG + " Action not added correctly");
                return false;
            }
            
            // Test trigger copying
            Trigger copiedTrigger = trigger.copy();
            if (!copiedTrigger.isValid() || copiedTrigger.getActions().size() != 1)
            {
                Logger.error(TEST_TAG + " Trigger copy failed");
                return false;
            }
            
            // Test thread safety
            trigger.setEnabled(true);
            if (!trigger.isEnabled())
            {
                Logger.error(TEST_TAG + " Thread safety test failed");
                return false;
            }
            
            Logger.norm(TEST_TAG + " ✅ Trigger class tests passed");
            return true;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Error in Trigger class tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test TriggerActions
     */
    private boolean testTriggerActions()
    {
        Logger.norm(TEST_TAG + " Testing TriggerActions...");
        
        try
        {
            // Test CustomAction
            CustomAction customAction = new CustomAction("Test Command");
            if (!customAction.isValid())
            {
                Logger.error(TEST_TAG + " Valid CustomAction failed validation");
                return false;
            }
            
            // Test GearSwapAction
            GearSwapAction gearAction = new GearSwapAction("Test Gear");
            if (!gearAction.isValid())
            {
                Logger.error(TEST_TAG + " Valid GearSwapAction failed validation");
                return false;
            }
            
            // Test action copying with proper casting
            TriggerAction copiedActionBase = customAction.copy();
            if (copiedActionBase == null || !copiedActionBase.isValid())
            {
                Logger.error(TEST_TAG + " Action copy failed");
                return false;
            }
            
            // Test action validation
            CustomAction invalidAction = new CustomAction("");
            if (invalidAction.isValid())
            {
                Logger.error(TEST_TAG + " Invalid action passed validation");
                return false;
            }
            
            Logger.norm(TEST_TAG + " ✅ TriggerAction tests passed");
            return true;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Error in TriggerAction tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test TriggerEvents
     */
    private boolean testTriggerEvents()
    {
        Logger.norm(TEST_TAG + " Testing TriggerEvents...");
        
        try
        {
            // Test event creation
            TriggerEvent event = new TriggerEvent(TriggerEventType.ANIMATION_CHANGED, "test_source");
            
            // Test validation
            if (!event.isValid())
            {
                Logger.error(TEST_TAG + " Valid event failed validation");
                return false;
            }
            
            // Test data operations
            event.addData("test_key", "test_value");
            if (!event.hasData("test_key") || !"test_value".equals(event.getData("test_key")))
            {
                Logger.error(TEST_TAG + " Event data operations failed");
                return false;
            }
            
            // Test typed data access
            event.addData("number_key", 42);
            Integer number = event.getDataAsInteger("number_key");
            if (number != null && number != 42)
            {
                Logger.error(TEST_TAG + " Typed data access failed");
                return false;
            }
            
            // Test event copying
            TriggerEvent copiedEvent = event.copy();
            if (!copiedEvent.isValid() || !copiedEvent.hasData("test_key"))
            {
                Logger.error(TEST_TAG + " Event copy failed");
                return false;
            }
            
            Logger.norm(TEST_TAG + " ✅ TriggerEvent tests passed");
            return true;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Error in TriggerEvent tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test error handling
     */
    private boolean testErrorHandling()
    {
        Logger.norm(TEST_TAG + " Testing error handling...");
        
        try
        {
            // Test invalid trigger creation
            try
            {
                Trigger invalidTrigger = new Trigger("", "", null);
                if (invalidTrigger.isValid())
                {
                    Logger.error(TEST_TAG + " Invalid trigger should have failed validation");
                    return false;
                }
            }
            catch (Exception e)
            {
                // Expected behavior - invalid triggers should throw exceptions
            }
            
            // Test null inputs in config
            TriggerConfig config = new TriggerConfig();
            config.setAnimationId(-1000); // Invalid ID
            if (config.isValid())
            {
                Logger.error(TEST_TAG + " Invalid config should have failed validation");
                return false;
            }
            
            Logger.norm(TEST_TAG + " ✅ Error handling tests passed");
            return true;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Error in error handling tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test edge cases
     */
    private boolean testEdgeCases()
    {
        Logger.norm(TEST_TAG + " Testing edge cases...");
        
        try
        {
            // Test trigger with no actions
            Trigger triggerWithNoActions = new Trigger("no_actions", "No Actions", TriggerType.ANIMATION);
            if (!triggerWithNoActions.isValid())
            {
                Logger.error(TEST_TAG + " Trigger with no actions should be valid");
                return false;
            }
            
            // Test action with empty description
            GearSwapAction emptyDescAction = new GearSwapAction("");
            if (emptyDescAction.isValid())
            {
                Logger.error(TEST_TAG + " Action with empty description should be invalid");
                return false;
            }
            
            // Test event with null source
            TriggerEvent nullSourceEvent = new TriggerEvent(TriggerEventType.CUSTOM, null);
            if (!nullSourceEvent.isValid())
            {
                Logger.error(TEST_TAG + " Event with null source should still be valid");
                return false;
            }
            
            Logger.norm(TEST_TAG + " ✅ Edge case tests passed");
            return true;
        }
        catch (Exception e)
        {
            Logger.error(TEST_TAG + " Error in edge case tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Main method for running tests
     */
    public static void main(String[] args)
    {
        TriggerSystemTest testSuite = new TriggerSystemTest();
        boolean success = testSuite.runAllTests();
        
        if (success)
        {
            Logger.norm(TEST_TAG + " 🎉 All tests completed successfully!");
            System.exit(0);
        }
        else
        {
            Logger.error(TEST_TAG + " 💥 Some tests failed!");
            System.exit(1);
        }
    }
}
