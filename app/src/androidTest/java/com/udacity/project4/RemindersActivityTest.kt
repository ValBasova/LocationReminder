package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.authentication.LoginViewModel
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    @Test
    fun saveReminderIfAuthenticated_snackBarShown() {
        // start up Reminders screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        val viewModel = dataBindingIdlingResource.activity.getViewModel<LoginViewModel>()

        if (viewModel.authenticationState.value == LoginViewModel.AuthenticationState.AUTHENTICATED) {

            // Add a reminder
            onView(withId(R.id.addReminderFAB)).perform(click())
            onView(withId(R.id.reminderTitle)).perform(typeText("TITLE1"))
            onView(withId(R.id.reminderDescription))
                .perform(typeText("DESCRIPTION"))
            onView(ViewMatchers.isRoot()).perform(closeSoftKeyboard())
            onView(withId(R.id.saveReminder)).perform(click())

            //SnackBar message asks to add a location
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(ViewAssertions.matches(ViewMatchers.withText("Please select location")))

            activityScenario.close()
        }
    }

    @Test
    fun saveReminderifAuthenticated_ToastShown() = runBlocking {
        // start up Reminders screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }

        val viewModel = dataBindingIdlingResource.activity.getViewModel<LoginViewModel>()

        if (viewModel.authenticationState.value == LoginViewModel.AuthenticationState.AUTHENTICATED) {
            // Add a reminder
            onView(withId(R.id.addReminderFAB)).perform(click())
            onView(withId(R.id.reminderTitle)).perform(typeText("TITLE1"))
            onView(withId(R.id.reminderDescription))
                .perform(typeText("DESCRIPTION"))
            onView(ViewMatchers.isRoot()).perform(closeSoftKeyboard())


            onView(withId(R.id.selectLocation)).perform(click())
            onView(withContentDescription("Google Map")).perform(longClick())
            onView(withId(R.id.save_button)).perform(click())

            onView(withId(R.id.saveReminder)).perform(click())

            onView(ViewMatchers.withText(R.string.reminder_saved)).inRoot(
                withDecorView(
                    not(activity?.window?.decorView)
                )
            )
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }


    @Test
    fun saveReminder_pressSave_enterTitleSnackBarShown() = runBlocking {
        // start up Reminders screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }

        val viewModel = dataBindingIdlingResource.activity.getViewModel<LoginViewModel>()

        if (viewModel.authenticationState.value == LoginViewModel.AuthenticationState.AUTHENTICATED) {
            // Add a reminder
            onView(withId(R.id.addReminderFAB)).perform(click())

            onView(withId(R.id.saveReminder)).perform(click())

            //SnackBar message asks to enter a title
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(ViewAssertions.matches(ViewMatchers.withText("Please enter title")))

        }
        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun saveReminder_locationEntered_toastEnterRemindersDetailsShown() = runBlocking {
        // start up Reminders screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }

        val viewModel = dataBindingIdlingResource.activity.getViewModel<LoginViewModel>()

        if (viewModel.authenticationState.value == LoginViewModel.AuthenticationState.AUTHENTICATED) {
            // Add a reminder
            onView(withId(R.id.addReminderFAB)).perform(click())

            onView(withId(R.id.selectLocation)).perform(click())
            onView(withContentDescription("Google Map")).perform(longClick())
            onView(withId(R.id.save_button)).perform(click())

            onView(withId(R.id.saveReminder)).perform(click())

            //Toast to ask user to enter reminder's details shown
            onView(ViewMatchers.withText(R.string.enterTitleAndDescription)).inRoot(
                withDecorView(
                    not(activity?.window?.decorView)
                )
            )
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        }
        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }
}
