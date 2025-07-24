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

            // Set the result in the saved state handle
            navController.previousBackStackEntry?.savedStateHandle?.set(
                "service_submission_result",
                mapOf(
                    "serviceName" to selectedServiceName,
                    "success" to true
                )
            )

            // Reset the view model state
            viewModel.setSubmissionSuccess(false)
            viewModel.setSubmitClicked(false)
            
            // Navigate back
            navController.popBackStack()
        }
    }

    LaunchedEffect(selectedServiceName) {
        viewModel.loadExistingService(selectedServiceName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp))
            .verticalScroll(rememberScrollState())
    ) {
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            TopBar(navController = navController, serviceName = selectedServiceName)

            Experience()

            Column(modifier = Modifier.padding(start = 38.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("Choose Sub Category")
                        withStyle(style = SpanStyle(color = Color.Red)) {
                            append("*")
                        }
                    },
                    modifier = Modifier.padding(bottom = 12.dp).offset(x = -16.dp),
                    fontSize = 16.sp,
                    fontFamily = JostRegular,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF3B3130)
                )

                SpecializationWithCharges(
                    viewModel = viewModel,
                    onSpecializationChanged = { updatedList ->
                        viewModel.onSpecializationChanged(updatedList)
                    },
                    onSubCategorySelected = selectedServiceName
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val availableWorkstyle = viewModel.availableWorkstyle
            val errorMessage = viewModel.workstyleErrorMessage
            val selectedWorkstyle by viewModel.selectedWorkstyle // Add this line

            PreferredWorkStyle(
                selectedWorkstyle = selectedWorkstyle, // Use the state value
                availableWorkstyle = viewModel.availableWorkstyle,
                onWorkstyleSelect = { workstyle ->
                    viewModel.toggleWorkstyleSelection(workstyle)
                    viewModel.validateWorkstyle()
                },
                errorMessage = viewModel.workstyleErrorMessage,
                isError = !viewModel.isWorkstyleValid && viewModel.workstyleErrorMessage.isNotEmpty(),
                showError = showValidationErrors,
                viewModel = viewModel,
                category = selectedServiceName
            )

            val selectedOptions by viewModel.selectedOptions.collectAsState()
            val selectedTimeSlots by viewModel.selectedTimeSlots.collectAsState()
            val showValidationErrors by viewModel.showValidationErrors.collectAsState()

            PreferredOptionList(
                title = "",
                options = listOf("Morning", "Afternoon", "Evening"),
                selectedOptions = selectedOptions, // Use the collected state
                onOptionSelected = { option ->
                    viewModel.toggleOption(option)
                },
                timeSlots = mapOf(
                    "Morning" to listOf("6 AM - 8 AM", "8 AM - 10 AM", "10 AM - 12 PM"),
                    "Afternoon" to listOf("12 PM - 2 PM", "2 PM - 4 PM"),
                    "Evening" to listOf("4 PM - 5 PM", "5 PM - 6 PM", "6 PM - 7 PM")
                ),
                selectedTimeSlots = selectedTimeSlots, // Use the collected state
                onTimeSlotSelected = { option, slot ->
                    viewModel.toggleTimeSlot(option, slot)
                },
                viewModel = viewModel,
                modifier = Modifier.padding(start = 13.dp).offset(y = (-15.dp)),
                showError = showValidationErrors,
            )

            // Optional error message
            val uiState = viewModel.uiState.collectAsState().value
            val isSubmittedSuccessfully = viewModel.isSubmittedSuccessfully.value

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Submit Button
                Box(
                    modifier = Modifier
                        .requiredWidth(160.dp)
                        .requiredHeight(40.dp)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                            shape = RoundedCornerShape(10.dp)
                            clip = true
                        }
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFB2B1FF),
                                    Color(0xFFC7F2FF)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Button(
                        onClick = {
                            Log.d("MyApplication", "Submit button clicked")
                            viewModel.onSubmitForm2()
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            disabledElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(),
                        enabled = !isSubmittedSuccessfully && !uiState.isLoading
                    ) {
                        Text(
                            text = "Submit",
                            color = Color(0xFF3B3130),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W400,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))

                // Success/Error Messages Overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .zIndex(2f)
                            .align(Alignment.TopCenter)
                            .offset(y = -350.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (viewModel.submissionSuccess.value) {
                            SuccessMessage(
                                label = "Your registration has been successfully completed!"
                            )
                        } else if (uiState.errorMessage != null) {
                            SubmitErrorMessage(
                                label = uiState.errorMessage ?: ""
                            )

                            LaunchedEffect(uiState.errorMessage) {
                                delay(3000)
                                viewModel.clearErrorMessage()
                            }
                        }
                    }
                }
            }
        }
    }
}