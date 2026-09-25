package hu.infokristaly.homework4timersonandroid

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.infokristaly.homework4timersonandroid.data.IntervalItemType
import hu.infokristaly.homework4timersonandroid.data.SavedIntervalItem
import hu.infokristaly.homework4timersonandroid.data.SavedIntervalList
import org.junit.Assert.assertEquals
import org.junit.Test

class PresetImportExportTest {

    private val gson = Gson()

    @Test
    fun testJsonSerializationAndDeserialization() {
        val originalPresets = listOf(
            SavedIntervalList(
                id = "p1",
                name = "Warmup & Run",
                createdAt = 1000L,
                items = listOf(
                    SavedIntervalItem(id = "i1", minutes = 2, label = "Warmup", itemType = IntervalItemType.INTERVAL),
                    SavedIntervalItem(id = "i2", minutes = 0, label = "", itemType = IntervalItemType.OPEN_BRACKET),
                    SavedIntervalItem(id = "i3", minutes = 5, label = "Sprint", itemType = IntervalItemType.INTERVAL),
                    SavedIntervalItem(id = "i4", minutes = 0, label = "", itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 3)
                )
            )
        )

        val json = gson.toJson(originalPresets)
        val type = object : TypeToken<List<SavedIntervalList>>() {}.type
        val deserialized: List<SavedIntervalList> = gson.fromJson(json, type)

        assertEquals(1, deserialized.size)
        assertEquals("Warmup & Run", deserialized[0].name)
        assertEquals(4, deserialized[0].items.size)
        assertEquals("Warmup", deserialized[0].items[0].label)
        assertEquals(3, deserialized[0].items[3].repeatCount)
    }

    @Test
    fun testImportRenamingLogicHungarian() {
        val existingNames = mutableSetOf("Futás", "Séta másolat")
        val importedNames = listOf("Futás", "Séta", "Futás")
        val suffix = " másolat"

        val resultNames = mutableListOf<String>()
        for (name in importedNames) {
            var newName = name.trim()
            while (existingNames.contains(newName)) {
                newName = "$newName$suffix"
            }
            existingNames.add(newName)
            resultNames.add(newName)
        }

        assertEquals("Futás másolat", resultNames[0])
        assertEquals("Séta", resultNames[1])
        assertEquals("Futás másolat másolat", resultNames[2])
    }

    @Test
    fun testImportRenamingLogicEnglish() {
        val existingNames = mutableSetOf("Running", "Running copied")
        val importedNames = listOf("Running", "Walking")
        val suffix = " copied"

        val resultNames = mutableListOf<String>()
        for (name in importedNames) {
            var newName = name.trim()
            while (existingNames.contains(newName)) {
                newName = "$newName$suffix"
            }
            existingNames.add(newName)
            resultNames.add(newName)
        }

        assertEquals("Running copied copied", resultNames[0])
        assertEquals("Walking", resultNames[1])
    }
}
