@Composable
fun ChargesInputUi(
    specialization: String,
    viewModel: SPViewModel2,
    selectedDurations: Map<String, Boolean>,
    onValidationError: (String) -> Unit,
) {
    val charges by remember {
        derivedStateOf {
            viewModel.chargesPerSpecialization[specialization] ?: Charges()
        }
    }

    val suffixMap = mapOf(
        "Per Hour" to "H",
        "Per Day" to "D",
        "Per Week" to "W",
        "Per Month" to "M"
    )

    val maxDigitsMap = mapOf(
        "Per Hour" to 4,
        "Per Day" to 4,
        "Per Week" to 5,
        "Per Month" to 5
    )

    val minDigitsMap = mapOf(
        "Per Hour" to 3,
        "Per Day" to 3,
        "Per Week" to 4,
        "Per Month" to 4
    )

    val submitClicked by viewModel.submitClicked.collectAsState()

    // 🧠 MODIFIED: Updated validation logic to check ALL selected workstyles
    val chargeValues = mapOf(
        "Hour" to charges.perHour,
        "Day" to charges.perDay,
        "Week" to charges.perWeek,
        "Month" to charges.perMonth
    )

    val hasAnySelected = selectedDurations.any { it.value }

    // 🔥 NEW VALIDATION: Check that ALL selected workstyles have valid charges
    val areAllSelectedValid = selectedDurations.all { (key, isSelected) ->
        if (!isSelected) return@all true // Skip unselected items
        
        val value = chargeValues[key] ?: ""
        val digits = value.filter { it.isDigit() }
        val minDigits = when (key) {
            "Hour", "Day" -> 3
            "Week", "Month" -> 4
            else -> 3
        }
        val minAmount = when (key) {
            "Hour", "Day" -> 100
            "Week", "Month" -> 1000
            else -> 100
        }
        
        // All selected items must have valid charges
        digits.isNotEmpty() && 
        digits.length >= minDigits && 
        (digits.toIntOrNull() ?: 0) >= minAmount
    }

    // Show error if no workstyle selected OR if any selected workstyle has invalid charges
    val showTopError = submitClicked && (!hasAnySelected || !areAllSelectedValid)

    Column(modifier = Modifier.padding(top = 9.dp, bottom = 4.dp)) {

        // 🔴 Highlight top label when invalid
        Text(
            text = buildAnnotatedString {
                append("Expected Service Charge")
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append("*")
                }
            },
            color = if (showTopError) Color(0xFFFF4D4D) else Color(0xFF3B3130),
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = JostRegular,
                fontWeight = FontWeight(400)
            ),
            modifier = Modifier.padding(bottom = 3.dp).offset(x=-16.dp)
        )

        Text(
            text = "(Enter your expected Charge)",
            color = Color(0xff3B3130).copy(alpha = 0.8f),
            style = TextStyle(fontSize = 12.sp, fontFamily = JostRegular),
            modifier = Modifier.padding(bottom = 14.dp).offset(x=-16.dp)
        )

        val chargeItems = listOf(
            Triple("Per Hour", charges.perHour) { newValue: String ->
                viewModel.updateCharges(specialization, charges.copy(perHour = newValue))
            },
            Triple("Per Day", charges.perDay) { newValue: String ->
                viewModel.updateCharges(specialization, charges.copy(perDay = newValue))
            },
            Triple("Per Week", charges.perWeek) { newValue: String ->
                viewModel.updateCharges(specialization, charges.copy(perWeek = newValue))
            },
            Triple("Per Month", charges.perMonth) { newValue: String ->
                viewModel.updateCharges(specialization, charges.copy(perMonth = newValue))
            }
        )

        for (i in chargeItems.indices step 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(50.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                for (j in 0..1) {
                    if (i + j < chargeItems.size) {
                        val (label, value, onChange) = chargeItems[i + j]
                        val suffix = suffixMap[label] ?: ""
                        val maxDigits = maxDigitsMap[label] ?: 5
                        val minDigits = minDigitsMap[label] ?: 3

                        val durationKey = label.removePrefix("Per ").trim()
                        val isSelected = selectedDurations[durationKey] ?: false
                        val digitsOnly = value.filter { it.isDigit() }
                        val isFocused = remember { mutableStateOf(false) }

                        // 🔥 UPDATED: More strict validation for individual fields
                        val minAmount = when (durationKey) {
                            "Hour", "Day" -> 100
                            "Week", "Month" -> 1000
                            else -> 100
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                    .border(1.dp, Color.Transparent, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(0.2f)
                                            .background(Color(0xFFDBDBFC)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.rupee),
                                            contentDescription = "Rupee Icon",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(0.8f)
                                            .background(Color(0xB2E6F3FC))
                                            .padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = digitsOnly,
                                            onValueChange = { newText ->
                                                if (isSelected) {
                                                    val digits = newText.filter { it.isDigit() }
                                                    if (digits.length <= maxDigits) {
                                                        onChange(digits)
                                                    }
                                                }
                                            },
                                            readOnly = !isSelected,
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 15.sp,
                                                fontFamily = JostRegular,
                                                color = if (isSelected) Color(0xFF3B3130) else Color.Gray
                                            ),
                                            modifier = Modifier
                                                .onFocusChanged { focusState ->
                                                    isFocused.value = focusState.isFocused
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            decorationBox = {
                                                if (digitsOnly.isEmpty()) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 15.sp,
                                                        fontFamily = JostRegular,
                                                        color = Color(0xCC3B3130)
                                                    )
                                                } else {
                                                    Text(
                                                        buildAnnotatedString {
                                                            withStyle(style = SpanStyle(
                                                                fontSize = 15.sp,
                                                                fontFamily = JostRegular,
                                                                color = if (isSelected) Color(0xFF3B3130) else Color.Gray
                                                            )) {
                                                                append(digitsOnly)
                                                            }
                                                            withStyle(style = SpanStyle(
                                                                fontSize = 15.sp,
                                                                fontFamily = JostRegular,
                                                                color = Color(0xFF3B3130)
                                                            )) {
                                                                append("/$suffix")
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // 🔥 UPDATED: Show error for selected fields that are invalid
                            val highlightError = isSelected && (
                                    (submitClicked && (digitsOnly.isEmpty() || 
                                                     digitsOnly.length < minDigits || 
                                                     (digitsOnly.toIntOrNull() ?: 0) < minAmount))
                                            || (digitsOnly.isNotEmpty() && (digitsOnly.length in 1 until minDigits || 
                                                                           (digitsOnly.toIntOrNull() ?: 0) < minAmount))
                                    )

                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            color = if (highlightError) Color(0xCCFF4D4D) else Color(0xCC3B3130)
                                        )
                                    ) {
                                        append("${digitsOnly.length}")
                                    }
                                    withStyle(style = SpanStyle(color = Color(0xCC3B3130))) {
                                        append("/$maxDigits")
                                    }
                                },
                                fontSize = 14.sp,
                                fontFamily = JostRegular,
                                modifier = Modifier.padding(top = 2.dp, start = 70.dp)
                            )

                        }
                    } else {
                        Spacer(modifier = Modifier.width(100.dp))
                    }
                }
            }
        }

        // 🔁 Trigger validation
        LaunchedEffect(charges) {
            validateCharges(viewModel, specialization, charges, onValidationError)
            viewModel.validateAndPotentiallyClearError()
        }
    }
}

// Helper function for validation (you may need to add this)
private fun validateCharges(
    viewModel: SPViewModel2,
    specialization: String,
    charges: Charges,
    onValidationError: (String) -> Unit
) {
    // This function can be used for additional validation if needed
    // The main validation is now handled in the ViewModel's validateCharges method
}