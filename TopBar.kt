@Composable
fun TopBar(navController: NavController,serviceName:String) {
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
                fontWeight = FontWeight.W500, // ✅ Font weight 500
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f) // Take all available horizontal space
                    .padding(end = 8.dp) // Space between text and image
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
                        // Use popBackStack instead of navigate to preserve the navigation stack
                        // This will go back to the previous screen (SPHome) without clearing the state
                        navController.popBackStack()
                    },
                    onNoClick = { showDialog = false }
                )
            }
        }
    }
}