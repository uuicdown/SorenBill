package com.soren.bill.ui.home

import com.soren.bill.data.entity.*
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `categoryMap combines expense and income categories`() {
        val expCats = listOf(Category(id = 1, name = "餐饮", type = "expense"))
        val incCats = listOf(Category(id = 2, name = "工资", type = "income"))
        val state = HomeUiState(expenseCategories = expCats, incomeCategories = incCats)
        assert(state.categoryMap[1L] == "餐饮")
        assert(state.categoryMap[2L] == "工资")
        assert(state.categoryMap.size == 2)
    }

    @Test
    fun `adjustmentCategoryIds returns only isAdjustment categories`() {
        val expCats = listOf(
            Category(id = 1, name = "餐饮", type = "expense"),
            Category(id = 2, name = "余额调整", type = "expense", isAdjustment = true)
        )
        val incCats = listOf(
            Category(id = 3, name = "工资", type = "income"),
            Category(id = 4, name = "余额调整", type = "income", isAdjustment = true)
        )
        val state = HomeUiState(expenseCategories = expCats, incomeCategories = incCats)
        assert(state.adjustmentCategoryIds == setOf(2L, 4L))
    }
}
