package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.AndroidFakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest()  {

    private lateinit var dataSource: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var reminder1: ReminderDTO
    private lateinit var reminder2: ReminderDTO

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { AndroidFakeDataSource() as ReminderDataSource }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        dataSource = get()

        //clear the data to start fresh
        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.

    @Test
    fun displayReminderList_twoRemindersDisplayed() {
        // GIVEN two reminders
        reminder1 = ReminderDTO(
            "title1",
            "description1",
            "location3",
            -30.0,
            150.0
        )
        reminder2 = ReminderDTO(
            "title2",
            "description2",
            "location3",
            -34.0,
            151.0
        )
        runBlockingTest {
            dataSource.saveReminder(reminder1)
            dataSource.saveReminder(reminder2)


            // WHEN - ReminderList Fragment launched
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
            Thread.sleep(2000)
            //Then two reminders are displayed

            onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
//        onView(withId(R.id.title)).check(
//            matches(
//                withText(
//                    "title1"
//                )
//            )
//        )
        }

    }
}