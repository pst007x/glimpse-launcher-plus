package com.glimpsemedia.glimpselauncher.model

/*
 * The bottom bar: up to three apps each side of the All apps button. The [Layout.dock] list runs left to right and
 * [Layout.dockLeft] of them sit on the left. With four places shown, each side shows the two apps nearest All apps;
 * any outer ones are kept, hidden, and come back when six places are shown again.
 */

private fun Layout.leftApps() = dock.take(dockLeft)            // outer to inner
private fun Layout.rightApps() = dock.drop(dockLeft)           // inner to outer

/** The apps shown left of All apps (outer to inner) and right of it (inner to outer) in a bar of [slots] places. */
fun Layout.dockSides(slots: Int): Pair<List<String>, List<String>> {
    val half = slots / 2
    return leftApps().takeLast(half) to rightApps().take(half)
}

/** The apps kept in the bar but not shown with [slots] places (the outer ones). */
fun Layout.dockHidden(slots: Int): List<String> {
    val half = slots / 2
    return leftApps().dropLast(half) + rightApps().drop(half)
}

/** Whether another app fits in a bar of [slots] places. */
fun Layout.dockHasRoom(slots: Int): Boolean = leftApps().size < slots / 2 || rightApps().size < slots / 2

/** Adds [key] to the first free place: the outer end of the left side, else the outer end of the right side. */
fun Layout.addToDock(key: String, slots: Int): Layout {
    val half = slots / 2
    return when {
        key in dock -> this
        leftApps().size < half -> copy(dock = listOf(key) + dock, dockLeft = dockLeft + 1)
        rightApps().size < half -> copy(dock = dock + key)
        else -> this
    }
}

private fun <T> List<T>.swapped(i: Int, j: Int): List<T> = toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }

/**
 * Moves [key] one place left ([by] −1) or right (+1) among the apps shown with [slots] places. Crossing the All apps
 * button takes a free place on the other side if there is one, else swaps with the app nearest All apps there.
 * The shown ends do not move, and a hidden app never comes into view: if leaving would uncover one, it swaps instead.
 */
fun Layout.dockMoved(key: String, by: Int, slots: Int): Layout {
    val half = slots / 2
    val (left, right) = dockSides(slots)
    val i = dock.indexOf(key)
    if (i < 0) return this
    if (by < 0) {
        val r = right.indexOf(key)
        val l = left.indexOf(key)
        return when {
            l > 0 -> copy(dock = dock.swapped(i, i - 1))
            r > 0 -> copy(dock = dock.swapped(i, i - 1))
            // A free place on the left is taken only if no hidden app on the right would come into view.
            r == 0 && left.size < half && rightApps().size <= half -> copy(dockLeft = dockLeft + 1)
            r == 0 && left.isNotEmpty() -> copy(dock = dock.swapped(i, i - 1))
            else -> this
        }
    } else {
        val l = left.indexOf(key)
        val r = right.indexOf(key)
        return when {
            r >= 0 && r < right.size - 1 -> copy(dock = dock.swapped(i, i + 1))
            l >= 0 && l < left.size - 1 -> copy(dock = dock.swapped(i, i + 1))
            l == left.size - 1 && l >= 0 && right.size < half && leftApps().size <= half -> copy(dockLeft = dockLeft - 1)
            l == left.size - 1 && l >= 0 && right.isNotEmpty() -> copy(dock = dock.swapped(i, i + 1))
            else -> this
        }
    }
}

fun Layout.canMoveInDock(key: String, by: Int, slots: Int): Boolean = dockMoved(key, by, slots) != this
