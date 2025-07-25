import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.widget.Toast

@Composable
fun SPHome(navController: NavController, viewModel: SPHomeViewModel) {
    val viewModel: SPHomeViewModel = viewModel(
        factory = SPHomeViewModelFactory(UserRepository())
    )
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Use rememberSaveable to persist across configuration changes and navigation
    val submittedServices = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    // Handle navigation results - this will trigger when returning from registration screen
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            // Check if we're returning to this screen
            if (destination.route == SERVICE_PROVIDER_REGISTRATION_HOME_SCRN) {
                // Get the current back stack entry
                val currentEntry = navController.currentBackStackEntry
                
                // Handle service submission result
                val submissionResult = currentEntry?.savedStateHandle?.get<Map<String, Any>>("service_submission_result")
                submissionResult?.let {
                    val serviceName = it["serviceName"] as? String
                    val success = it["success"] as? Boolean ?: false
                    val isEdit = it["isEdit"] as? Boolean ?: false

                    if (success && serviceName != null && !submittedServices.contains(serviceName)) {
                        submittedServices.add(serviceName)
                    }

                    // Clear the result after processing
                    currentEntry.savedStateHandle.remove<Map<String, Any>>("service_submission_result")
                }
                
                // Handle service deletion result
                val deletionResult = currentEntry?.savedStateHandle?.get<Map<String, Any>>("service_deletion_result")
                deletionResult?.let {
                    val serviceName = it["serviceName"] as? String
                    val deleted = it["deleted"] as? Boolean ?: false

                    if (deleted && serviceName != null && submittedServices.contains(serviceName)) {
                        submittedServices.remove(serviceName)
                    }

                    // Clear the result after processing
                    currentEntry.savedStateHandle.remove<Map<String, Any>>("service_deletion_result")
                }
            }
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // Also handle the case where the result is already present when composing
    LaunchedEffect(Unit) {
        val currentEntry = navController.currentBackStackEntry
        
        // Handle submission result
        val submissionResult = currentEntry?.savedStateHandle?.get<Map<String, Any>>("service_submission_result")
        submissionResult?.let {
            val serviceName = it["serviceName"] as? String
            val success = it["success"] as? Boolean ?: false
            val isEdit = it["isEdit"] as? Boolean ?: false

            if (success && serviceName != null && !submittedServices.contains(serviceName)) {
                submittedServices.add(serviceName)
            }

            // Clear the result after processing
            currentEntry.savedStateHandle.remove<Map<String, Any>>("service_submission_result")
        }
        
        // Handle deletion result
        val deletionResult = currentEntry?.savedStateHandle?.get<Map<String, Any>>("service_deletion_result")
        deletionResult?.let {
            val serviceName = it["serviceName"] as? String
            val deleted = it["deleted"] as? Boolean ?: false

            if (deleted && serviceName != null && submittedServices.contains(serviceName)) {
                submittedServices.remove(serviceName)
            }

            // Clear the result after processing
            currentEntry.savedStateHandle.remove<Map<String, Any>>("service_deletion_result")
        }
    }

    SetStatusBarColorForHome()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Header(
            userId = "",
            navController = navController,
            onNotificationClick = { /* handle notifications */ },
            viewModel = viewModel,
            modifier = Modifier
        )
//        Spacer(modifier = Modifier.height(10.dp))
        CategoriesSection(
            navController = navController,
            submittedServices = submittedServices,
            onServiceSubmitted = { serviceName, success ->
                if (success && !submittedServices.contains(serviceName)) {
                    submittedServices.add(serviceName)
                }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        UpcomingServicesSection()
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sphome_banner_2),
                contentDescription = "Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 70.dp, y = -48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Small image above text
                Image(
                    painter = painterResource(id = R.drawable.img_32),
                    contentDescription = "Support Icon",
                    modifier = Modifier
                        .size(30.dp) // smaller icon
                        .padding(top = 7.dp) // minimal space before text
                )
                // Click-to-copy email text
                Text(
                    text = "support@clanhub.in",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Thin,
                    fontFamily = league_spartan,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString("support@clanhub.in"))
                        Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}