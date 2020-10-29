/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * Data access object for the [ClickDatabase].
 *
 * Provide methods to access/edit the click database. Structures available in the database tables can be found in
 * Entities.kt.
 * Actual implementation is automatically generated by AndroidX Room.
 */
@Dao
internal abstract class ClickDao {

    /**
     * Get all click scenario and their clicks.
     *
     * @return the live data on the list of scenarios.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table ORDER BY name ASC")
    abstract fun getClickScenarios(): LiveData<List<ScenarioWithClicks>>

    /**
     * Add a new scenario to the database.
     *
     * @param scenarioEntity the scenario to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addClickScenario(scenarioEntity: ScenarioEntity)

    /**
     * Rename the selected scenario.
     *
     * @param id the identifier of the scenario to be renamed
     * @param name the new name of the scenario
     */
    @Query("UPDATE scenario_table SET name=:name WHERE id=:id")
    abstract suspend fun renameScenario(id: Long, name: String)

    /**
     * Get all clicks and their conditions from the database for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to get the clicks for.
     *
     * @return the live data for the list of clicks of the specified scenario.
     */
    @Transaction
    @Query("SELECT * FROM click_table WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract fun getClicksWithConditions(scenarioId: Long): LiveData<List<ClickWithConditions>>

    /**
     * Get all clicks and their conditions from the database for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to get the clicks for.
     *
     * @return the list of clicks of the specified scenario.
     */
    @Transaction
    @Query("SELECT * FROM click_table WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract suspend fun getClicksWithConditionsList(scenarioId: Long): List<ClickWithConditions>

    /**
     * Get the count of clicks for a given scenario.
     *
     * @param scenarioId the scenario to get the count of clicks.
     *
     * @return the count of clicks.
     */
    @Query("SELECT COUNT(*) FROM click_table WHERE scenario_id=:scenarioId")
    abstract suspend fun getClickCount(scenarioId: Long): Int

    /**
     * Get all the clicks with a lower priority in a scenario.
     *
     * @param scenarioId the scenario of the clicks
     * @param priority the priority, all clicks with a priority value higher than this will be returned.
     *
     * @return the corresponding list of clicks.
     */
    @Query("SELECT * FROM click_table WHERE scenario_id=:scenarioId AND priority>:priority ORDER BY priority")
    abstract suspend fun getClicksLessPrioritized(scenarioId: Long, priority: Int): List<ClickEntity>

    /**
     * Add a new click with its conditions to the database.
     *
     * @param entity the click to be added.
     */
    @Transaction
    open suspend fun addClickWithConditions(entity: ClickWithConditions) {
        val clickId = addClick(entity.click)
        addClickConditions(clickId, entity.conditions)
    }

    /**
     * Update the database value of all clicks provided (and only the clicks, not its conditions).
     *
     * @param clickEntities the list of clicks to update.
     */
    @Update
    abstract suspend fun updateClicks(clickEntities: List<ClickEntity>)

    /**
     * Update the database value of the provided click and its conditions.
     *
     * @param entity the click to be updated.
     */
    open suspend fun updateClickWithConditions(entity: ClickWithConditions) {
        updateClick(entity.click)

        val oldConditions = getClickConditions(entity.click.clickId).conditions
        // Create and add new conditions
        addClickConditions(entity.click.clickId, entity.conditions.minus(oldConditions))
        // Remove old ones
        deleteClickConditionsCrossRefs(oldConditions.minus(entity.conditions).map {
            ClickConditionCrossRef(entity.click.clickId, it.path)
        })
    }

    /**
     * Delete the provided click scenario from the database.
     *
     * Any associated [ClickEntity] will be removed from the database, as well as their related [ClickConditionCrossRef]
     * due to the [androidx.room.ForeignKey.CASCADE] deletion of this parent scenario.
     *
     * You might want to call [deleteClicklessConditions] after this method in order to avoid useless conditions to be
     * stored in the database.
     *
     * @param scenario the scenario to be deleted.
     */
    @Delete
    abstract suspend fun deleteClickScenario(scenario: ScenarioEntity)

    /**
     * Delete the provided click from the database.
     *
     * Any associated [ClickConditionCrossRef] will be removed from the database as well due to the
     * [androidx.room.ForeignKey.CASCADE] deletion of this parent click.
     *
     * You might want to call [deleteClicklessConditions] after this method in order to avoid useless conditions to be
     * stored in the database.
     *
     * @param click the click to be deleted.
     */
    @Delete
    abstract suspend fun deleteClick(click: ClickEntity)

    /**
     * Get all conditions without at least one click associated from the database.
     * Once conditions external data have been cleaned, you must call [deleteClicklessConditions] to remove them from the
     * database.
     *
     * @return the livedata on the list of condition without a click.
     */
    @Transaction
    @Query("SELECT * FROM condition_table WHERE path NOT IN(SELECT path FROM ClickConditionCrossRef)")
    abstract fun getClicklessConditions(): LiveData<List<ConditionEntity>>

    /**
     * Delete all click condition without at least one click associated from the database.
     *
     * Any associated [ClickConditionCrossRef] will be removed from the database as well due to the
     * [androidx.room.ForeignKey.CASCADE] deletion of this parent condition.
     */
    @Transaction
    @Query("DELETE FROM condition_table WHERE path NOT IN(SELECT path FROM ClickConditionCrossRef)")
    abstract suspend fun deleteClicklessConditions()

    /**
     * Get the provided click, with its conditions from the database.
     *
     * @param clickId the identifier of the click to get.
     *
     * @return the click with its list of conditions.
     */
    @Transaction
    @Query("SELECT * FROM click_table WHERE clickid=:clickId")
    protected abstract fun getClickConditions(clickId: Long): ClickWithConditions

    /**
     * Add a click to the database.
     *
     * @param clickEntity the click to be added. As the identifier is automatically generated by the database, always
     * use 0 as identifier for the click to allow the generation.
     *
     * @return the identifier for the newly created click.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addClick(clickEntity: ClickEntity): Long

    /**
     * Add a list of conditions to the database and attach them to the provided click.
     *
     * If a condition is already in the database, insertion will be skipped, but the conditions will still be attached
     * to the click.
     *
     * @param clickId the identifier of first click related to the conditions
     * @param conditions the list of conditions to be added.
     */
    protected open suspend fun addClickConditions(clickId: Long, conditions: List<ConditionEntity>) {
        addConditions(conditions) // Skip the entities already in the base
        addClickConditionsCrossRefs(conditions.map { ClickConditionCrossRef(clickId, it.path) })
    }

    /**
     * Add a list of conditions to the database.
     *
     * Note that using this method will not link the conditions to any clicks. See [addClickConditionsCrossRefs] for
     * that.
     *
     * @param conditions the conditions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addConditions(conditions: List<ConditionEntity>)

    /**
     * Add a list of cross references between a click and a condition to the database.
     *
     * In order to succeed, all conditions and clicks must have been added to the database.
     *
     * @param crossRefs the list of cross references to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addClickConditionsCrossRefs(crossRefs: List<ClickConditionCrossRef>)

    /**
     * Update the database value of a click (with its conditions)
     *
     * @param click the click to be updated.
     */
    @Update
    protected abstract suspend fun updateClick(click: ClickEntity)

    /**
     * Delete a list of conditions to clicks cross reference from the database.
     *
     * The provided conditions will no longer be attached to their provided clicks.
     *
     * @param crossRef the list of cross references to be removed from the database.
     */
    @Delete
    protected abstract suspend fun deleteClickConditionsCrossRefs(crossRef: List<ClickConditionCrossRef>)
}