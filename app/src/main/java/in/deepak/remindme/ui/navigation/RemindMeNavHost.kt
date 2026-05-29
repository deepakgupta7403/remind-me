package `in`.deepak.remindme.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.ui.LocalAppContainer
import `in`.deepak.remindme.ui.screens.edit.ReminderEditScreen
import `in`.deepak.remindme.ui.screens.edit.ReminderEditViewModel
import `in`.deepak.remindme.ui.screens.home.HomeScreen
import `in`.deepak.remindme.ui.screens.home.HomeViewModel
import `in`.deepak.remindme.ui.screens.onboarding.OnboardingScreen
import `in`.deepak.remindme.ui.screens.profile.ProfileScreen
import `in`.deepak.remindme.ui.screens.profile.ProfileViewModel
import `in`.deepak.remindme.ui.screens.search.SearchScreen
import `in`.deepak.remindme.ui.screens.search.SearchViewModel
import `in`.deepak.remindme.ui.screens.settings.SettingsScreen
import `in`.deepak.remindme.ui.screens.stats.StatsScreen
import `in`.deepak.remindme.ui.screens.stats.StatsViewModel
import `in`.deepak.remindme.ui.screens.templates.TemplatesScreen
import `in`.deepak.remindme.ui.screens.templates.TemplatesViewModel

/**
 * Hosts every screen. ViewModels are constructed via [viewModelFactory] with
 * dependencies pulled from [LocalAppContainer], so screens stay framework-light
 * and the container is the single wiring point.
 *
 * Bottom-nav tabs (Home/Stats/Templates/Settings) each live at the top level
 * here so the system back stack behaves naturally — tapping Stats then Home
 * doesn't pile up; we use `popUpTo(Home) { saveState = true }` semantics.
 */
@Composable
fun RemindMeNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val container = LocalAppContainer.current

    val startRoute = if (container.onboardingPreferences.hasCompleted) {
        Destination.Home.route
    } else {
        Destination.Onboarding.route
    }

    fun selectTab(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(Destination.Home.route) { saveState = true; inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {

        composable(Destination.Onboarding.route) {
            OnboardingScreen(
                onCompleted = { name ->
                    container.userPreferences.userName = name
                    container.onboardingPreferences.hasCompleted = true
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Destination.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        HomeViewModel(
                            repository = container.reminderRepository,
                            scheduler = container.alarmScheduler,
                            userPreferences = container.userPreferences,
                        )
                    }
                }
            )
            HomeScreen(
                viewModel = viewModel,
                onAddReminder = { navController.navigate(Destination.Edit.routeForNew()) },
                onEditReminder = { id -> navController.navigate(Destination.Edit.routeFor(id)) },
                onSearch = { navController.navigate(Destination.Search.route) },
                onBrowseTemplates = { selectTab(Destination.Templates) },
                onTabSelected = ::selectTab,
            )
        }

        composable(Destination.Search.route) {
            val searchViewModel: SearchViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SearchViewModel(
                            repository = container.reminderRepository,
                            scheduler = container.alarmScheduler,
                            searchPreferences = container.searchPreferences,
                            templateRepository = container.templateRepository,
                        )
                    }
                }
            )
            SearchScreen(
                viewModel = searchViewModel,
                onBack = { navController.popBackStack() },
                onResultTap = { id -> navController.navigate(Destination.Edit.routeFor(id)) },
                onToggle = searchViewModel::setEnabled,
            )
        }

        composable(Destination.Stats.route) {
            val statsViewModel: StatsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        StatsViewModel(
                            repository = container.reminderRepository,
                            activityLog = container.reminderActivityLog,
                        )
                    }
                }
            )
            StatsScreen(
                viewModel = statsViewModel,
                onTabSelected = ::selectTab,
            )
        }

        composable(Destination.Templates.route) {
            val templatesViewModel: TemplatesViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        TemplatesViewModel(
                            repository = container.reminderRepository,
                            scheduler = container.alarmScheduler,
                            templateRepository = container.templateRepository,
                        )
                    }
                }
            )
            TemplatesScreen(
                viewModel = templatesViewModel,
                onTabSelected = ::selectTab,
            )
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                onTabSelected = ::selectTab,
                onOpenProfile = { navController.navigate(Destination.Profile.route) },
            )
        }

        composable(Destination.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ProfileViewModel(preferences = container.userPreferences)
                    }
                }
            )
            ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Destination.Edit.route,
            arguments = listOf(
                navArgument(Destination.Edit.ARG_ID) {
                    type = NavType.LongType
                    defaultValue = Reminder.UNSAVED_ID
                }
            ),
        ) { backStackEntry ->
            val viewModel: ReminderEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val handle: SavedStateHandle = createSavedStateHandle()
                        // Surface nav arg into SavedStateHandle so it survives recreation.
                        handle[ReminderEditViewModel.KEY_REMINDER_ID] =
                            backStackEntry.arguments?.getLong(Destination.Edit.ARG_ID)
                                ?: Reminder.UNSAVED_ID
                        ReminderEditViewModel(
                            repository = container.reminderRepository,
                            scheduler = container.alarmScheduler,
                            savedStateHandle = handle,
                        )
                    }
                }
            )
            ReminderEditScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
