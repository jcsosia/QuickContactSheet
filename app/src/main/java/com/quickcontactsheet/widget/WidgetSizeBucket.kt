package com.quickcontactsheet.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WidgetSizeBucket {
    SingleColumn,
    TwoByOne,
    ThreeByOne,
    FourPlusByOne,
    TwoByTwo,
    Large,
    ;

    companion object {
        fun from(size: DpSize): WidgetSizeBucket {
            val columns = when {
                size.width < 110.dp -> 1
                size.width < 190.dp -> 2
                size.width < 270.dp -> 3
                else -> 4
            }
            val rows = if (size.height < 115.dp) 1 else 2
            return when {
                columns <= 1 -> SingleColumn
                rows == 1 && columns == 2 -> TwoByOne
                rows == 1 && columns == 3 -> ThreeByOne
                rows == 1 && columns >= 4 -> FourPlusByOne
                columns == 2 && rows == 2 -> TwoByTwo
                else -> Large
            }
        }
    }
}
