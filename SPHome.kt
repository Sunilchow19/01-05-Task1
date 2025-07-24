@Composable
fun SPHome(navController: NavController, viewModel: SPHomeViewModel ) {
    val viewModel: SPHomeViewModel = viewModel(
        factory = SPHomeViewModelFactory(UserRepository())
    )
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val submittedServices = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    // Use a more stable approach to handle navigation results
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val result = backStackEntry.savedStateHandle.get<Map<String, Any>>("service_submission_result")
            result?.let {
                val serviceName = it["serviceName"] as? String
                val success = it["success"] as? Boolean ?: false
                
                if (success && serviceName != null && !submittedServices.contains(serviceName)) {
                    submittedServices.add(serviceName)
                }

                // Clear result after processing to avoid reprocessing
                backStackEntry.savedStateHandle.remove<Map<String, Any>>("service_submission_result")
            }
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