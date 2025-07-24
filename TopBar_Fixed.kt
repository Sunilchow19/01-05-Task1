@Composable
fun TopBar(navController: NavController, serviceName: String) {
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 25.dp, start = 25.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${serviceName} Service",
                color = Color.Black,
                fontSize = 18.sp,
                fontFamily = JostMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDialog = true }
                    .padding(4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.go_back),
                    contentDescription = "Go Back",
                    modifier = Modifier
                        .width(95.dp)
                        .height(25.dp)
                )
            }
        }

        if (showDialog) {
            Dialog(
                onDismissRequest = { showDialog = false },
                properties = DialogProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                )
            ) {
                UnSavedPopUp(
                    onYesClick = {
                        showDialog = false
                        // FIXED: Use popBackStack instead of navigate to preserve the navigation stack
                        // This will go back to the previous screen (SPHome) without clearing the state
                        navController.popBackStack()
                    },
                    onNoClick = { showDialog = false }
                )
            }
        }
    }
}

@Composable
fun UnSavedPopUp(
    onYesClick: () -> Unit,
    onNoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressedButton by remember { mutableStateOf<PressedButton?>(null) }

    Dialog(
        onDismissRequest = { onNoClick() },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .width(380.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
            ) {
                // Close "X" button
                IconButton(
                    onClick = { onNoClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_vector),
                        contentDescription = "Close Popup",
                        tint = Color.Gray
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left-side image
                    Box(
                        modifier = Modifier
                            .size(78.dp, 150.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_30),
                            contentDescription = "Warning Icon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                    }

                    // Right content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 25.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF3B3130),
                                        fontSize = 15.sp,
                                        fontFamily = JostRegular
                                    )
                                ) {
                                    append("Your changes are not saved. Are you sure you want to Discard")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF3B3130),
                                        fontSize = 15.sp,
                                        fontFamily = JostRegular
                                    )
                                ) {
                                    append(" the changes?")
                                }
                            },
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // No button
                            Button(
                                onClick = {
                                    pressedButton = PressedButton.NO
                                    onNoClick()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pressedButton == PressedButton.NO)
                                        Color(0xFFE5C3FF).copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    contentColor = Color(0xFF3B3130)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE5C3FF)),
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = "No",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontFamily = JostMedium
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(60.dp))

                            // Yes button
                            Button(
                                onClick = {
                                    pressedButton = PressedButton.YES
                                    onYesClick()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pressedButton == PressedButton.YES)
                                        Color(0xFFE5C3FF).copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    contentColor = Color(0xFF3B3130)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE5C3FF)),
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = "Yes",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontFamily = JostMedium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class PressedButton {
    YES, NO
}