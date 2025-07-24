@OptIn(ExperimentalPagerApi::class)
@SuppressLint("StateFlowValueCalledInComposition", "SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ServiceProviderRegistrationScreen2(
    navController: NavController,
    viewModel: SPViewModel2,
    selectedServiceName : String
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val submissionSuccess = viewModel.submissionSuccess.value
    val errorMessage = viewModel.errorMessage.value
    val firebaseRepository = FirebaseRepositorys()

    val state = viewModel.uiState.collectAsState().value
    var selectedGender by remember { mutableStateOf("") }
    var emergencyPhoneNumber by remember { mutableStateOf("") }
    val selectedTimeSlots = remember { mutableStateMapOf<String, String>() }
    var selectedPerHourCharge by remember { mutableStateOf(" Per Hour") }
    var selectedPerDayCharge by remember { mutableStateOf("Per Day") }
    var selectedPerWeekCharge by remember { mutableStateOf(" Per Week") }
    var selectedPerMonthCharge by remember { mutableStateOf("Per Month") }
    var selectedCharges by remember { mutableStateOf(mapOf<String, String>()) }

    var selectedLanguages by remember { mutableStateOf(listOf<String>()) }
    var languageErrorMessage by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    val chargesValidationError by viewModel.chargesValidationError.collectAsState()
    val showValidationErrors by viewModel.showValidationErrors.collectAsState()
    var dismissError by remember { mutableStateOf(false) }

    val activities = context.findActivity() as? MainActivity

    val locationViewModel: LocationViewModel = viewModel(
        factory = LocationViewModel.LocationViewModelFactory(activities!!.locationService)
    )

    var showBackDialog by remember { mutableStateOf(false) }
    var locationScreenResult by remember { mutableStateOf(false) }

    BackHandler {
        showBackDialog = true
    }

    // Handle successful submission and navigation
    LaunchedEffect(submissionSuccess) {
        if (submissionSuccess) {
            delay(1000) // Brief delay to show success message

            // Navigate back to previous screen with success result
            navController.previousBackStackEntry?.savedStateHandle?.set(
                "service_submission_result",
                mapOf(
                    "serviceName" to selectedServiceName,
                    "success" to true
                )
            )

            // Reset submission success before navigating
            viewModel.setSubmissionSuccess(false)
            viewModel.setSubmitClicked(false)
            
            navController.popBackStack()
        }
    }

    // Remove the second LaunchedEffect that was causing issues
    // The error handling should not interfere with successful submissions

    LaunchedEffect(selectedServiceName) {
        viewModel.loadExistingService(selectedServiceName)
    }

// ... existing code ...