/*
 * Copyright (C) 2023 Kevin Buzeau
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.tutorial.domain.model

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.feature.tutorial.data.StepEndCondition
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialStepData
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.monitoring.TutorialMonitoredViewType


data class TutorialOverlayState(
    @StringRes val tutorialInstructionsResId: Int,
    val hideFloatingUi: Boolean,
    val stepEnd: TutorialStepEnd,
)

sealed class TutorialStepEnd {

    object NextButton : TutorialStepEnd()

    data class MonitoredViewClick(
        val type: TutorialMonitoredViewType,
    ) : TutorialStepEnd()
}

internal fun TutorialStepData.toDomain(): TutorialOverlayState =
    TutorialOverlayState(
        tutorialInstructionsResId = contentTextResId,
        hideFloatingUi = hideFloatingUi,
        stepEnd = stepEndCondition.toDomain(),
    )

private fun StepEndCondition.toDomain(): TutorialStepEnd =
    when (this) {
        StepEndCondition.NextButton -> TutorialStepEnd.NextButton
        is StepEndCondition.MonitoredViewClicked -> TutorialStepEnd.MonitoredViewClick(type)
    }