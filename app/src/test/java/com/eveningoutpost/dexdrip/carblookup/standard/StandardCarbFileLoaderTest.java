package com.eveningoutpost.dexdrip.carblookup.standard;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StandardCarbFileLoaderTest extends RobolectricTestWithConfig {

    private File testDir;
    private File testFile;

    @Before
    public void setUpLoader() {
        // Use Robolectric's simulated /sdcard/xdrip
        testDir = new File(android.os.Environment.getExternalStorageDirectory(), "xdrip");
        testDir.mkdirs();
        testFile = new File(testDir, "common_foods.json");
        StandardCarbFileLoader.invalidateCache();
    }

    @After
    public void tearDown() {
        if (testFile.exists()) testFile.delete();
        StandardCarbFileLoader.invalidateCache();
    }

    @Test
    public void mergeWithDefaults_noFile_returnsDefaultsUnchanged() {
        if (testFile.exists()) testFile.delete();
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        assertEquals(defaults.size(), result.size());
        assertEquals("Rice", result.get(0).name);
    }

    @Test
    public void mergeWithDefaults_userFileAddsItems() throws Exception {
        writeJson("[{\"category\":\"FRUIT\",\"name\":\"Kiwi\",\"carbsPer100g\":14.7," +
                "\"smallPortion\":60,\"mediumPortion\":90,\"largePortion\":130,\"source\":\"User\"}]");
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        assertEquals(defaults.size() + 1, result.size());
        StandardCarbItem kiwi = findByName(result, "Kiwi");
        assertNotNull(kiwi);
        assertEquals(StandardCarbCategory.FRUIT, kiwi.category);
        assertEquals(14.7, kiwi.carbsPer100g, 0.01);
        assertEquals(90.0, kiwi.mediumPortionGrams, 0.01);
        assertEquals("User", kiwi.source);
    }

    @Test
    public void mergeWithDefaults_userFileOverridesExistingItem() throws Exception {
        writeJson("[{\"category\":\"STARCHES_GRAINS\",\"name\":\"Rice\",\"carbsPer100g\":30.0," +
                "\"smallPortion\":80,\"mediumPortion\":160,\"largePortion\":240,\"source\":\"My data\"}]");
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        // Same size (override, not addition)
        assertEquals(defaults.size(), result.size());
        StandardCarbItem rice = findByName(result, "Rice");
        assertEquals(30.0, rice.carbsPer100g, 0.01);
        assertEquals(160.0, rice.mediumPortionGrams, 0.01);
        assertEquals("My data", rice.source);
    }

    @Test
    public void mergeWithDefaults_liquidItemFromJson() throws Exception {
        writeJson("[{\"category\":\"DAIRY\",\"name\":\"Almond milk\",\"carbsPer100g\":1.5," +
                "\"smallPortion\":100,\"mediumPortion\":200,\"largePortion\":300," +
                "\"source\":\"User\",\"portionUnit\":\"ml\",\"gramsPerMl\":1.03}]");
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        StandardCarbItem almond = findByName(result, "Almond milk");
        assertNotNull(almond);
        assertEquals(StandardCarbItem.UNIT_MILLILITERS, almond.portionUnit);
        assertEquals(1.03, almond.gramsPerPortionUnit, 0.01);
    }

    @Test
    public void mergeWithDefaults_invalidJson_returnsDefaults() throws Exception {
        writeJson("not valid json at all {{{");
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        assertEquals(defaults.size(), result.size());
    }

    @Test
    public void mergeWithDefaults_unknownCategory_skipsItem() throws Exception {
        writeJson("[{\"category\":\"MADE_UP_CATEGORY\",\"name\":\"Mystery\",\"carbsPer100g\":5," +
                "\"smallPortion\":10,\"mediumPortion\":20,\"largePortion\":30,\"source\":\"User\"}]");
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        assertEquals(defaults.size(), result.size());
    }

    @Test
    public void mergeWithDefaults_categoryByLabel_parsesLeniently() throws Exception {
        writeJson("[{\"category\":\"Fruit\",\"name\":\"Papaya\",\"carbsPer100g\":11.0," +
                "\"smallPortion\":80,\"mediumPortion\":150,\"largePortion\":250,\"source\":\"User\"}]");
        List<StandardCarbItem> defaults = sampleDefaults();

        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        StandardCarbItem papaya = findByName(result, "Papaya");
        assertNotNull(papaya);
        assertEquals(StandardCarbCategory.FRUIT, papaya.category);
    }

    @Test
    public void writeSeedFile_createsValidJson() {
        if (testFile.exists()) testFile.delete();
        List<StandardCarbItem> defaults = sampleDefaults();

        StandardCarbFileLoader.writeSeedFile(defaults);

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 10);
        // Loading it back should produce the same items
        StandardCarbFileLoader.invalidateCache();
        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(new ArrayList<>());
        assertEquals(defaults.size(), result.size());
        assertEquals("Rice", findByName(result, "Rice").name);
        assertEquals(28.0, findByName(result, "Rice").carbsPer100g, 0.01);
    }

    @Test
    public void invalidateCache_forcesReload() throws Exception {
        writeJson("[{\"category\":\"FRUIT\",\"name\":\"Fig\",\"carbsPer100g\":19.0," +
                "\"smallPortion\":30,\"mediumPortion\":50,\"largePortion\":80,\"source\":\"User\"}]");
        List<StandardCarbItem> defaults = sampleDefaults();
        StandardCarbFileLoader.mergeWithDefaults(defaults); // populates cache

        // Overwrite file with different content
        writeJson("[{\"category\":\"FRUIT\",\"name\":\"Date\",\"carbsPer100g\":75.0," +
                "\"smallPortion\":10,\"mediumPortion\":30,\"largePortion\":50,\"source\":\"User\"}]");
        // Without invalidation, would still return Fig
        StandardCarbFileLoader.invalidateCache();
        List<StandardCarbItem> result = StandardCarbFileLoader.mergeWithDefaults(defaults);

        assertNotNull(findByName(result, "Date"));
    }

    private List<StandardCarbItem> sampleDefaults() {
        List<StandardCarbItem> items = new ArrayList<>();
        items.add(new StandardCarbItem(StandardCarbCategory.STARCHES_GRAINS, "Rice", 28.0,
                100, 150, 230, "Test source"));
        items.add(new StandardCarbItem(StandardCarbCategory.FRUIT, "Apple", 13.8,
                80, 150, 220, "Test source"));
        return items;
    }

    private void writeJson(String json) throws Exception {
        testDir.mkdirs();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(testFile), StandardCharsets.UTF_8)) {
            w.write(json);
        }
    }

    private StandardCarbItem findByName(List<StandardCarbItem> items, String name) {
        for (StandardCarbItem item : items) {
            if (name.equals(item.name)) return item;
        }
        return null;
    }
}
