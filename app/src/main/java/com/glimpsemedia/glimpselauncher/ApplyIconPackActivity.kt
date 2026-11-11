package com.glimpsemedia.glimpselauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.glimpsemedia.glimpselauncher.data.IconPacks
import com.glimpsemedia.glimpselauncher.data.PendingIconPack
import com.glimpsemedia.glimpselauncher.model.acceptApply
import com.glimpsemedia.glimpselauncher.model.applyExtraKey

/**
 * Answers the "Apply" a few icon packs send without naming a launcher: asks "Use <pack> icons in Glimpse?" and, only on
 * Use them, hands the pack to the home screen. Anything that is not an installed icon pack is ignored quietly.
 */
class ApplyIconPackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packs = IconPacks(this)
        val installed = packs.installed()
        // Only the one extra this action names its pack in is read, as text; anything odd reads as nothing.
        val key = applyExtraKey(intent.action)
        val extras = key?.let { k -> mapOf(k to try { intent.getStringExtra(k) } catch (_: Exception) { null }) }.orEmpty()
        val pkg = acceptApply(intent.action, extras) { p -> installed.any { it.pkg == p } }
        if (pkg == null) { finish(); return }
        val name = installed.first { it.pkg == pkg }.label
        AlertDialog.Builder(this)
            .setMessage("Use $name icons in Glimpse?")
            .setPositiveButton("Use them") { _, _ ->
                PendingIconPack.offer(pkg)
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
