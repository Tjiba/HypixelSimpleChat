package com.simplechat.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class EntryKind { BOOLEAN, INT, STRING, ENUM, COLOR }

/** Une option : valeur courante + métadonnées. L'id = nom de la propriété Kotlin. */
class ConfigEntry(
    val id: String,
    val kind: EntryKind,
    val title: String,
    val description: String,
    val default: Any,
    val enumConstants: List<Enum<*>> = emptyList(),
) {
    var value: Any = default

    fun getBoolean() = value as Boolean
    fun setBoolean(v: Boolean) { value = v }
    fun getInt() = value as Int
    fun setInt(v: Int) { value = v }
    fun getString() = value as String
    fun setString(v: String) { value = v }
    fun getEnum() = value as Enum<*>
    fun setEnum(v: Enum<*>) { value = v }
}

class EntryMeta {
    var name = ""
    var description = ""
}

/** Groupe d'options (racine ou catégorie). Chaque delegate enregistre son entrée à la déclaration. */
open class ConfigGroup {
    val entries = LinkedHashMap<String, ConfigEntry>()

    private fun <T : Any> register(kind: EntryKind, default: T, constants: List<Enum<*>>, block: EntryMeta.() -> Unit) =
        PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> { _, prop ->
            val meta = EntryMeta().apply(block)
            val entry = ConfigEntry(prop.name, kind, meta.name, meta.description, default, constants)
            entries[prop.name] = entry
            object : ReadWriteProperty<Any?, T> {
                @Suppress("UNCHECKED_CAST")
                override fun getValue(thisRef: Any?, property: KProperty<*>) = entry.value as T
                override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { entry.value = value }
            }
        }

    protected fun boolean(default: Boolean, block: EntryMeta.() -> Unit = {}) = register(EntryKind.BOOLEAN, default, emptyList(), block)
    protected fun int(default: Int, block: EntryMeta.() -> Unit = {}) = register(EntryKind.INT, default, emptyList(), block)
    protected fun string(default: String, block: EntryMeta.() -> Unit = {}) = register(EntryKind.STRING, default, emptyList(), block)
    protected fun color(default: Int, block: EntryMeta.() -> Unit = {}) = register(EntryKind.COLOR, default, emptyList(), block)
    protected fun <T : Enum<T>> enum(default: T, block: EntryMeta.() -> Unit = {}) =
        register(EntryKind.ENUM, default, default.declaringJavaClass.enumConstants.toList(), block)
}

open class HscCategory(val id: String) : ConfigGroup()

/**
 * Config racine : entrées + catégories, persistée en JSON via Gson (fourni par Minecraft).
 * Lit aussi l'ancien format ResourcefulConfig (.jsonc, couleurs "#RRGGBB") puis réécrit en .json.
 */
abstract class HscConfig(path: String) : ConfigGroup() {
    private val logger = LoggerFactory.getLogger("simplechat")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = FabricLoader.getInstance().configDir.resolve("$path.json")
    private val legacyFile = FabricLoader.getInstance().configDir.resolve("$path.jsonc")

    val categories = LinkedHashMap<String, HscCategory>()

    protected fun category(cat: HscCategory) { categories[cat.id] = cat }

    fun load() {
        val source = file.takeIf { Files.exists(it) } ?: legacyFile.takeIf { Files.exists(it) }
        if (source == null) { save(); return }
        runCatching {
            // parseString est lenient : tolère les commentaires // du jsonc legacy.
            val root = JsonParser.parseString(Files.readString(source)).asJsonObject
            readGroup(this, root)
            for ((id, cat) in categories) {
                val obj = root.get(id) as? JsonObject ?: continue
                readGroup(cat, obj)
            }
        }.onFailure { logger.warn("Invalid config file {}, keeping defaults: {}", source.fileName, it.message) }
        save() // normalise (migration jsonc → json, nouvelles options)
    }

    fun resetToDefaults() {
        for (e in entries.values) e.value = e.default
        for (c in categories.values) for (e in c.entries.values) e.value = e.default
    }

    /** Applique un preset (même format JSON que le fichier config) par-dessus les défauts. */
    fun applyPreset(jsonText: String) {
        resetToDefaults()
        runCatching {
            val root = JsonParser.parseString(jsonText).asJsonObject
            readGroup(this, root)
            for ((id, cat) in categories) {
                val obj = root.get(id) as? JsonObject ?: continue
                readGroup(cat, obj)
            }
        }.onFailure { logger.warn("Invalid preset: {}", it.message) }
    }

    fun save() {
        val root = JsonObject()
        writeGroup(this, root)
        for ((id, cat) in categories) {
            val obj = JsonObject()
            writeGroup(cat, obj)
            root.add(id, obj)
        }
        runCatching {
            Files.createDirectories(file.parent)
            Files.writeString(file, gson.toJson(root))
        }.onFailure { logger.error("Failed to save config", it) }
    }

    private fun readGroup(group: ConfigGroup, json: JsonObject) {
        for (entry in group.entries.values) {
            val el = json.get(entry.id) ?: continue
            // Valeur illisible (type changé, enum renommé) → on garde le défaut.
            runCatching { entry.value = parseValue(entry, el) }
        }
    }

    private fun parseValue(entry: ConfigEntry, el: JsonElement): Any = when (entry.kind) {
        EntryKind.BOOLEAN -> el.asBoolean
        EntryKind.INT -> el.asInt
        EntryKind.STRING -> el.asString
        EntryKind.ENUM -> entry.enumConstants.first { it.name == el.asString }
        EntryKind.COLOR ->
            if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt
            else el.asString.removePrefix("#").toLong(16).toInt() // legacy RC : "#RRGGBB"
    }

    private fun writeGroup(group: ConfigGroup, json: JsonObject) {
        for (e in group.entries.values) when (e.kind) {
            EntryKind.BOOLEAN -> json.addProperty(e.id, e.getBoolean())
            EntryKind.INT, EntryKind.COLOR -> json.addProperty(e.id, e.getInt())
            EntryKind.STRING -> json.addProperty(e.id, e.getString())
            EntryKind.ENUM -> json.addProperty(e.id, e.getEnum().name)
        }
    }
}
