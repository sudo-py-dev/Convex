package com.convex.app.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A typed, validated parameter for an FFmpeg operation form.
 * Each variant maps to a specific Compose component.
 */
sealed class OperationParam {
    abstract val id: String

    @get:StringRes abstract val labelRes: Int
    abstract val required: Boolean

    /** Renders as OutlinedButton + file name Chip */
    data class FilePicker(
        override val id: String,
        @StringRes override val labelRes: Int,
        override val required: Boolean = true,
        val mimeTypes: List<String> = listOf("*/*"),
    ) : OperationParam()

    /** Renders as labeled Slider */
    data class SliderParam(
        override val id: String,
        @StringRes override val labelRes: Int,
        override val required: Boolean = true,
        val min: Float,
        val max: Float,
        val steps: Int = 0,
        val defaultValue: Float,
        /** Format string for value display, e.g. "%.0f" */
        val valueFormat: String = "%.0f",
    ) : OperationParam()

    /** Renders as ExposedDropdownMenuBox */
    data class DropdownParam(
        override val id: String,
        @StringRes override val labelRes: Int,
        override val required: Boolean = true,
        /** Pair<displayValue, ffmpegValue> */
        val options: List<Pair<String, String>>,
        val defaultIndex: Int = 0,
    ) : OperationParam()

    /** Renders as OutlinedTextField */
    data class TextParam(
        override val id: String,
        @StringRes override val labelRes: Int,
        override val required: Boolean = false,
        @StringRes val hintRes: Int? = null,
        val defaultValue: String = "",
        val singleLine: Boolean = true,
    ) : OperationParam()

    /** Renders as Row { Text + Switch } */
    data class SwitchParam(
        override val id: String,
        @StringRes override val labelRes: Int,
        override val required: Boolean = false,
        val defaultValue: Boolean = false,
    ) : OperationParam()
}

/** A single FFmpeg operation (e.g. "Convert Format", "Trim Video"). */
data class Operation(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector,
    val params: List<OperationParam>,
    /** Builds the FFmpeg argument list from resolved param values. */
    val commandBuilder: (values: Map<String, String>) -> List<String>,
)

/** A top-level category grouping related operations. */
data class Category(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val operations: List<Operation>,
)
