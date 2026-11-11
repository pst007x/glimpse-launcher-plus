package com.glimpsemedia.glimpselauncher.model

import org.json.JSONArray
import org.json.JSONObject

/** The layout as text for saving, and back. Anything unreadable gives null, so the caller starts afresh. */
object LayoutJson {
    private const val VERSION = 1

    fun encode(layout: Layout): String {
        val pages = JSONArray()
        layout.pages.forEach { page ->
            val items = JSONArray()
            page.items.forEach { item ->
                items.put(
                    when (item) {
                        null -> JSONObject().put("t", "empty")
                        is Item.App -> JSONObject().put("t", "app").put("k", item.key)
                        Item.AllApps -> JSONObject().put("t", "all")
                        Item.Search -> JSONObject().put("t", "search")
                        Item.Emergency -> JSONObject().put("t", "emergency")
                        is Item.Widget -> JSONObject().put("t", "widget").put("id", item.id).put("p", item.provider)
                            .put("c", item.cols).put("r", item.rows)
                        Item.Covered -> JSONObject().put("t", "cover")
                        is Item.Folder -> JSONObject().put("t", "folder").put("id", item.id).put("name", item.name)
                            .put("apps", JSONArray(item.apps))
                    },
                )
            }
            pages.put(items)
        }
        val colors = JSONObject()
        layout.tileColors.forEach { (key, argb) -> colors.put(key, argb) }
        val see = JSONObject()
        layout.tileTransparency.forEach { (key, percent) -> see.put(key, percent) }
        val icons = JSONObject()
        layout.iconColors.forEach { (key, argb) -> icons.put(key, argb) }
        val iconSee = JSONObject()
        layout.iconTransparency.forEach { (key, percent) -> iconSee.put(key, percent) }
        val tags = JSONArray()
        layout.tags.forEach { tags.put(JSONObject().put("n", it.name).put("apps", JSONArray(it.apps.toList()))) }
        return JSONObject().put("v", VERSION).put("pages", pages).put("dock", JSONArray(layout.dock)).put("dl", layout.dockLeft)
            .put("colors", colors).put("see", see).put("icons", icons).put("iconSee", iconSee).put("tags", tags).put("cols", layout.columns).put("rows", layout.rows)
            .put("folderColors", JSONObject().also { o -> layout.folderColors.forEach { (k, v) -> o.put(k, v) } })
            .put("folderBadges", JSONObject().also { o -> layout.folderBadges.forEach { (k, v) -> o.put(k, v) } })
            .put("tileShade", JSONObject().also { o -> layout.tileShades.forEach { (k, v) -> o.put(k, v) } })
            .put("texts", JSONObject().also { o -> layout.textColors.forEach { (k, v) -> o.put(k, v) } })
            .put("textShade", JSONObject().also { o -> layout.textShades.forEach { (k, v) -> o.put(k, v) } })
            .put("iconShade", JSONObject().also { o -> layout.iconShades.forEach { (k, v) -> o.put(k, v) } })
            .put("hold", JSONObject().also { o -> layout.holdActions.forEach { (key, action) -> o.put(key, encodeHold(action)) } }).toString()
    }

    fun decode(text: String): Layout? = try {
        val root = JSONObject(text)
        if (root.getInt("v") != VERSION) null else {
            // A save from before the grid setting is 2×4.
            // A save from before the smaller grids has 4 rows; a grid that isn't one of ours is 2×4.
            val (cols, rows) = (root.optInt("cols", 2) to root.optInt("rows", PAGE_ROWS)).takeIf { it in GRIDS } ?: (2 to PAGE_ROWS)
            val pages = root.getJSONArray("pages").let { arr ->
                (0 until arr.length()).map { i ->
                    val items = arr.getJSONArray(i)
                    Page((0 until items.length()).mapNotNull { j -> decodeSlot(items.getJSONObject(j)) }.map { it.value }, cols, rows).packedEnd()
                }
            }
            val dock = root.getJSONArray("dock").let { arr -> (0 until arr.length()).map { arr.getString(it) } }
            val colors = root.optJSONObject("colors")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }.orEmpty()
            val see = root.optJSONObject("see")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }.orEmpty()
            val icons = root.optJSONObject("icons")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }.orEmpty()
            val iconSee = root.optJSONObject("iconSee")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }.orEmpty()
            val tags = root.optJSONArray("tags")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val t = arr.getJSONObject(i)
                    AppTag(t.getString("n"), t.getJSONArray("apps").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
                }
            }.orEmpty()
            // An action this version doesn't know is left out; the rest of the layout still reads.
            val hold = root.optJSONObject("hold")?.let { o ->
                o.keys().asSequence().mapNotNull { k -> o.optJSONObject(k)?.let { decodeHold(it) }?.let { k to it } }.toMap()
            }.orEmpty()
            fun shades(name: String) = root.optJSONObject(name)?.let { o ->
                o.keys().asSequence().associateWith { o.getInt(it).coerceIn(SHADE_MIN, SHADE_MAX) }.filterValues { it != 0 }
            }.orEmpty()
            // Before the six-place bar there was no "dl": the first two apps were on the left.
            val dl = root.optInt("dl", minOf(2, dock.size)).coerceIn(maxOf(0, dock.size - DOCK_SIDE_MAX), minOf(DOCK_SIDE_MAX, dock.size))
            val texts = root.optJSONObject("texts")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }.orEmpty().filterValues { it != 0 }
            // A layout from before folder colours has none: every folder has the shared colour.
            val folderColors = root.optJSONObject("folderColors")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } }.orEmpty().filterValues { it != 0 }
            // Badges with a name this version does not know are left out.
            val badgeNames = TextIcon.entries.map { it.name }.toSet().let { names -> names + names.map { COLOUR_BADGE + it } }
            val folderBadges = root.optJSONObject("folderBadges")?.let { o -> o.keys().asSequence().associateWith { o.optString(it) } }.orEmpty().filterValues { it in badgeNames }
            Layout(pages, dock, colors, see, icons, iconSee, tags, cols, hold, shades("tileShade"), shades("iconShade"), dl, texts, shades("textShade"), rows, folderColors, folderBadges)
        }
    } catch (_: Exception) {
        null
    }

    private fun encodeHold(a: HoldAction): JSONObject = when (a) {
        is HoldAction.OpenApp -> JSONObject().put("t", "app").put("k", a.key)
        is HoldAction.Web -> JSONObject().put("t", "web").put("u", a.url)
        is HoldAction.Call -> JSONObject().put("t", "call").put("n", a.number)
        is HoldAction.Text -> JSONObject().put("t", "text").put("n", a.number)
        is HoldAction.Phone -> JSONObject().put("t", "phone").put("p", a.what.name)
        HoldAction.Notes -> JSONObject().put("t", "notes")
    }

    private fun decodeHold(o: JSONObject): HoldAction? = when (o.optString("t")) {
        "app" -> o.optString("k").takeIf { it.isNotEmpty() }?.let { HoldAction.OpenApp(it) }
        "web" -> webAddress(o.optString("u"))?.let { HoldAction.Web(it) }
        "call" -> dialable(o.optString("n"))?.let { HoldAction.Call(it) }
        "text" -> dialable(o.optString("n"))?.let { HoldAction.Text(it) }
        "notes" -> HoldAction.Notes
        "phone" -> PhoneAction.entries.firstOrNull { it.name == o.optString("p") }?.let { HoldAction.Phone(it) }
        else -> null
    }

    /** A slot that was read: its item, or null for an empty slot. Unknown kinds give no slot at all. */
    private class Slot(val value: Item?)

    private fun decodeSlot(o: JSONObject): Slot? = if (o.optString("t") == "empty") Slot(null) else decodeItem(o)?.let { Slot(it) }

    private fun decodeItem(o: JSONObject): Item? = when (o.optString("t")) {
        "app" -> Item.App(o.getString("k"))
        "all" -> Item.AllApps
        "search" -> Item.Search
        "emergency" -> Item.Emergency
        "widget" -> Item.Widget(o.getInt("id"), o.getString("p"), o.getInt("c"), o.getInt("r"))
        "cover" -> Item.Covered
        "folder" -> Item.Folder(
            o.getString("id"), o.getString("name"),
            o.getJSONArray("apps").let { a -> (0 until a.length()).map { a.getString(it) } },
        )
        else -> null
    }
}
