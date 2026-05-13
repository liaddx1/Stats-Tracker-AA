package com.liad.statstracker.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.liad.statstracker.data.PerformanceRepository
import com.liad.statstracker.domain.PerformanceType

class PerformanceMenuCarScreen(carContext: CarContext) : Screen(carContext) {

    private val perfRepo = PerformanceRepository.get(carContext)

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder().apply {
            PerformanceType.entries.forEach { type ->
                val subtitle = if (type.isFromZero) {
                    "Standing start · 3s countdown"
                } else {
                    "Rolling · auto-trigger at ${type.entryKmh.toInt()} km/h"
                }
                addItem(
                    Row.Builder()
                        .setTitle(type.displayName)
                        .addText(subtitle)
                        .setOnClickListener {
                            perfRepo.start(type)
                            screenManager.push(PerformanceRunCarScreen(carContext))
                        }
                        .build()
                )
            }
        }.build()

        return ListTemplate.Builder()
            .setTitle("MEASURE")
            .setHeaderAction(Action.BACK)
            .setSingleList(list)
            .build()
    }
}
