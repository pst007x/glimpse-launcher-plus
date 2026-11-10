package com.glimpsemedia.glimpselauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import com.glimpsemedia.glimpselauncher.model.Topic

/** One launchable app. [key] is the flattened component name, the same text the saved layout uses. */
data class AppInfo(
    val key: String,
    val label: String,
    val component: ComponentName,
    /** What kind of app this is, for the related apps in a search (browser, social, photos...). */
    val topics: Set<Topic> = emptySet(),
)

class AppRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    /** Every app with a launcher icon, except this one, A to Z. */
    fun loadApps(): List<AppInfo> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val byIntent = topicsByIntent()
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                val component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                val pkg = it.activityInfo.packageName
                val topics = buildSet {
                    addAll(byIntent[pkg].orEmpty())
                    categoryTopic(it.activityInfo.applicationInfo.category)?.let { t -> add(t) }
                }
                AppInfo(component.flattenToString(), it.loadLabel(pm).toString(), component, topics)
            }
            .sortedBy { it.label.lowercase() }
    }

    /** What the app says it is (Android's own categories). */
    private fun categoryTopic(category: Int): Topic? = when (category) {
        ApplicationInfo.CATEGORY_SOCIAL -> Topic.SOCIAL
        ApplicationInfo.CATEGORY_AUDIO -> Topic.MUSIC
        ApplicationInfo.CATEGORY_VIDEO -> Topic.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> Topic.PHOTOS
        ApplicationInfo.CATEGORY_GAME -> Topic.GAMES
        ApplicationInfo.CATEGORY_NEWS -> Topic.NEWS
        ApplicationInfo.CATEGORY_MAPS -> Topic.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> Topic.OFFICE
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> Topic.ACCESSIBILITY
        else -> null
    }

    /** What an app does, from what it can handle: web pages are browsers, "mailto:" is email, and so on. */
    private fun topicsByIntent(): Map<String, Set<Topic>> {
        val found = HashMap<String, MutableSet<Topic>>()
        fun add(intent: Intent, topic: Topic) {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach {
                found.getOrPut(it.activityInfo.packageName) { mutableSetOf() }.add(topic)
            }
        }
        add(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE), Topic.INTERNET)
        add(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")), Topic.EMAIL)
        add(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")), Topic.MESSAGES)
        add(Intent(Intent.ACTION_DIAL), Topic.PHONE)
        add(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), Topic.PHOTOS)
        add(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY), Topic.PHOTOS)
        add(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=cafe")), Topic.MAPS)
        return found
    }

    /** The phone's own Phone, Messages, Gallery and Camera apps, in that order, as far as it names them. */
    fun preferredDockKeys(apps: List<AppInfo>): List<String> {
        fun byIntent(intent: Intent): String? {
            val pkg = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            return apps.firstOrNull { it.component.packageName == pkg }?.key
        }
        // The app called Gallery first (Samsung's own); otherwise whatever the phone says its gallery is.
        val gallery = apps.firstOrNull { it.label == "Gallery" }?.key
            ?: byIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY))
        return listOfNotNull(
            byIntent(Intent(Intent.ACTION_DIAL)),
            byIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            gallery,
            byIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
        ).distinct()
    }

    /** Text mode's starting apps: the phone's default dialler, messaging, gallery and browser (null where it has none). */
    fun textModeDefaults(apps: List<AppInfo>): List<String?> {
        fun byIntent(intent: Intent): String? {
            val pkg = try { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName } catch (_: Exception) { null }
            return apps.firstOrNull { it.component.packageName == pkg }?.key
        }
        val gallery = apps.firstOrNull { it.label == "Gallery" }?.key
            ?: byIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY))
        val browser = byIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
            ?: byIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")))
        return listOf(
            byIntent(Intent(Intent.ACTION_DIAL)),
            byIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
                ?: byIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)),
            gallery,
            browser,
        )
    }

    fun launch(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // The app was removed or disabled a moment ago; the next refresh drops it from the screen.
        }
    }
}
