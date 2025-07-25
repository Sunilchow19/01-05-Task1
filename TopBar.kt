import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController

@Composable
fun TopBar(
    navController: NavController, 
    serviceName: String,
    isEditMode: Boolean = false,
    onDeleteService: () -> Unit = {}
) {
    var showBackDialog by remember { mutableStateOf(false) }
    var showDeletePopup by remember { mutableStateOf(false) }
    var menuButtonPosition by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Three dots menu - only show in edit mode
                if (isEditMode) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showDeletePopup = !showDeletePopup }
                                .onGloballyPositioned { coordinates ->
                                    menuButtonPosition = IntOffset(
                                        coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero).x.toInt(),
                                        coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero).y.toInt()
                                    )
                                }
                        )
                        
                        // Delete popup
                        if (showDeletePopup) {
                            Popup(
                                offset = IntOffset(-120, 30), // Adjust position relative to the three dots
                                onDismissRequest = { showDeletePopup = false },
                                properties = PopupProperties(
                                    dismissOnClickOutside = false // Only dismiss when clicking the three dots again
                                )
                            ) {
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .padding(4.dp),
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(Color.White)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Delete Service",
                                            color = Color.Red,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.W500,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showDeletePopup = false
                                                    onDeleteService()
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Go back button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showBackDialog = true }
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
        }

        if (showBackDialog) {
            Dialog(
                onDismissRequest = { showBackDialog = false },
                properties = DialogProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                )
            ) {
                UnSavedPopUp(
                    onYesClick = {
                        showBackDialog = false
                        navController.popBackStack()
                    },
                    onNoClick = { showBackDialog = false }
                )
            }
        }
    }
}