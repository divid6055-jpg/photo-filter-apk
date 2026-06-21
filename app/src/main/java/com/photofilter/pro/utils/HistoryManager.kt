package com.photofilter.pro.utils

import java.util.Stack

/**
 * مدير السجل للتراجع والإعادة.
 * يحفظ نسخ من EditorState في كل تغيير.
 */
class HistoryManager(private val maxSize: Int = 50) {

    private val undoStack: Stack<EditorState> = Stack()
    private val redoStack: Stack<EditorState> = Stack()
    private var currentState: EditorState = EditorState()

    /** يحفظ الحالة الحالية قبل تعديل جديد */
    fun pushState(state: EditorState) {
        undoStack.push(currentState.copy())
        currentState = state.copy()
        redoStack.clear()
        // الحدّ الأقصى للسجل
        while (undoStack.size > maxSize) {
            undoStack.removeAt(0)
        }
    }

    /** يرجع للحالة السابقة - يعيد الحالة الجديدة أو null إذا لا يوجد سجل */
    fun undo(): EditorState? {
        if (undoStack.isEmpty()) return null
        redoStack.push(currentState.copy())
        currentState = undoStack.pop().copy()
        return currentState
    }

    /** يعيد التراجع - يعيد الحالة الجديدة أو null إذا لا يوجد */
    fun redo(): EditorState? {
        if (redoStack.isEmpty()) return null
        undoStack.push(currentState.copy())
        currentState = redoStack.pop().copy()
        return currentState
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /** يحدّث الحالة الحالية بدون حفظ في السجل (للحالات المؤقتة) */
    fun setCurrentState(state: EditorState) {
        currentState = state.copy()
    }

    fun getCurrentState(): EditorState = currentState

    /** يفرّغ السجل بالكامل */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        currentState = EditorState()
    }
}

/**
 * Presets جاهزة - مجموعات من الإعدادات + فلتر لمظهر معين.
 * يمكن للمستخدم اختيارها بنقرة واحدة.
 */
data class FilterPreset(
    val name: String,
    val displayNameRes: Int,
    val filter: FilterType,
    val adjust: AdjustSettings = AdjustSettings.DEFAULT
)

/**
 * قائمة الـ Presets الجاهزة في التطبيق.
 */
object PresetsManager {

    val presets: List<FilterPreset> = listOf(
        FilterPreset(
            name = "auto_enhance",
            displayNameRes = com.photofilter.pro.R.string.preset_auto_enhance,
            filter = FilterType.NONE,
            adjust = AdjustSettings(
                brightness = 10f, contrast = 15f, saturation = 10f, sharpness = 30f
            )
        ),
        FilterPreset(
            name = "portrait",
            displayNameRes = com.photofilter.pro.R.string.preset_portrait,
            filter = FilterType.WARM,
            adjust = AdjustSettings(
                brightness = 8f, contrast = 5f, saturation = 8f, warmth = 15f, sharpness = 20f
            )
        ),
        FilterPreset(
            name = "landscape",
            displayNameRes = com.photofilter.pro.R.string.preset_landscape,
            filter = FilterType.VIVID,
            adjust = AdjustSettings(
                contrast = 15f, saturation = 25f, sharpness = 35f, highlights = -10f, shadows = 15f
            )
        ),
        FilterPreset(
            name = "food",
            displayNameRes = com.photofilter.pro.R.string.preset_food,
            filter = FilterType.GOLDEN,
            adjust = AdjustSettings(
                brightness = 10f, saturation = 20f, warmth = 20f, sharpness = 25f
            )
        ),
        FilterPreset(
            name = "night_mode",
            displayNameRes = com.photofilter.pro.R.string.preset_night,
            filter = FilterType.NIGHT,
            adjust = AdjustSettings(
                brightness = 15f, contrast = 10f, shadows = 25f, grain = 10f
            )
        ),
        FilterPreset(
            name = "vintage_film",
            displayNameRes = com.photofilter.pro.R.string.preset_vintage,
            filter = FilterType.VINTAGE,
            adjust = AdjustSettings(
                contrast = 10f, saturation = -10f, grain = 20f, vignette = 25f
            )
        ),
        FilterPreset(
            name = "bw_dramatic",
            displayNameRes = com.photofilter.pro.R.string.preset_bw_drama,
            filter = FilterType.NOIR,
            adjust = AdjustSettings(
                contrast = 20f, sharpness = 30f, vignette = 20f
            )
        ),
        FilterPreset(
            name = "cinematic",
            displayNameRes = com.photofilter.pro.R.string.preset_cinematic,
            filter = FilterType.CINEMATIC,
            adjust = AdjustSettings(
                contrast = 15f, saturation = -5f, shadows = 20f, vignette = 15f
            )
        )
    )
}
