package ca.alexgirard.harpoonIJ;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.xmlb.XmlSerializer;

/** The plugin's persisted settings: defaults, and that every field actually round-trips. */
public class AppSettingsStateTest extends BasePlatformTestCase {

    public void testTheSettingsServiceIsRegistered() {
        assertNotNull(AppSettingsState.getInstance());
    }

    public void testTheSettingsServiceIsASingleton() {
        assertSame(AppSettingsState.getInstance(), AppSettingsState.getInstance());
    }

    public void testDefaults() {
        var settings = new AppSettingsState();

        assertEquals(800, settings.dialogWidth);
        assertEquals(400, settings.dialogHeight);
        assertEquals(20, settings.dialogFontSize);
        assertTrue("enter should select an entry out of the box", settings.enterRemap);
    }

    public void testGetStateReturnsTheComponentItself() {
        var settings = new AppSettingsState();

        assertSame(settings, settings.getState());
    }

    public void testLoadStateCopiesEveryField() {
        var saved = new AppSettingsState();
        saved.dialogWidth = 1234;
        saved.dialogHeight = 567;
        saved.dialogFontSize = 42;
        saved.enterRemap = false;

        var loaded = new AppSettingsState();
        loaded.loadState(saved);

        assertEquals(1234, loaded.dialogWidth);
        assertEquals(567, loaded.dialogHeight);
        assertEquals(42, loaded.dialogFontSize);
        assertFalse(loaded.enterRemap);
    }

    public void testSettingsSurviveAnXmlRoundTrip() {
        var saved = new AppSettingsState();
        saved.dialogWidth = 1000;
        saved.dialogHeight = 900;
        saved.dialogFontSize = 14;
        saved.enterRemap = false;

        var element = XmlSerializer.serialize(saved);
        var restored = XmlSerializer.deserialize(element, AppSettingsState.class);

        assertEquals(1000, restored.dialogWidth);
        assertEquals(900, restored.dialogHeight);
        assertEquals(14, restored.dialogFontSize);
        assertFalse(restored.enterRemap);
    }

    public void testAnEmptyStoredStateFallsBackToTheDefaults() {
        var element = XmlSerializer.serialize(new AppSettingsState());

        var restored = XmlSerializer.deserialize(element, AppSettingsState.class);

        assertEquals(800, restored.dialogWidth);
        assertEquals(400, restored.dialogHeight);
        assertEquals(20, restored.dialogFontSize);
        assertTrue(restored.enterRemap);
    }
}
