# Service Provider Registration Edit Mode Implementation

## Overview
This implementation adds edit mode functionality to the Service Provider Registration system, allowing users to edit existing services and delete them when needed.

## Key Features Implemented

### 1. Edit Mode Detection
- **ViewModel Enhancement**: Added `isEditMode` state to track whether the user is editing an existing service
- **Service Loading**: `loadExistingService()` method checks if a service already exists and sets edit mode accordingly
- **Form Population**: When in edit mode, existing service data is loaded into the form fields

### 2. Dynamic Button Text
- **Submit Button**: Changes text based on mode:
  - New service: "Submit"
  - Editing service: "Save Changes"
- **Success Messages**: Different messages for different actions:
  - New service: "Your registration has been successfully completed!"
  - Editing service: "Changes saved successfully!"

### 3. Delete Service Functionality
- **Three-Dots Menu**: Added to TopBar component, only visible in edit mode
- **Delete Popup**: Small popup appears next to the three-dots icon with "Delete Service" option
- **Popup Behavior**: Only closes when clicking the three-dots icon again (not on outside click)
- **Service Removal**: Deletes service permanently and removes it from the home page display

### 4. Navigation State Management
- **Result Handling**: Both submission and deletion results are passed back to the home screen
- **State Persistence**: Home screen updates the `submittedServices` list based on results
- **Clean Navigation**: Proper cleanup of navigation state after processing results

## Files Modified

### 1. SPViewModel2.kt
```kotlin
// New properties for edit mode
private val _isEditMode = MutableStateFlow(false)
val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

private val _deleteServiceSuccess = MutableStateFlow(false)
val deleteServiceSuccess: StateFlow<Boolean> = _deleteServiceSuccess.asStateFlow()

// New methods
fun setEditMode(isEditing: Boolean)
fun loadExistingService(serviceName: String)
fun deleteService(serviceName: String)
fun resetDeleteServiceSuccess()
```

### 2. TopBar.kt
```kotlin
@Composable
fun TopBar(
    navController: NavController, 
    serviceName: String,
    isEditMode: Boolean = false,        // New parameter
    onDeleteService: () -> Unit = {}    // New parameter
)
```
- Added three-dots menu (MoreVert icon) that only shows in edit mode
- Implemented delete popup with proper positioning
- Added delete service callback

### 3. ServiceProviderRegistrationScreen2.kt
```kotlin
// New state collection
val isEditMode by viewModel.isEditMode.collectAsState()
val deleteServiceSuccess by viewModel.deleteServiceSuccess.collectAsState()

// Modified TopBar call
TopBar(
    navController = navController, 
    serviceName = selectedServiceName,
    isEditMode = isEditMode,
    onDeleteService = {
        viewModel.deleteService(selectedServiceName)
    }
)

// Dynamic button text
Text(
    text = if (isEditMode) "Save Changes" else "Submit",
    // ... other properties
)

// Dynamic success message
SuccessMessage(
    label = if (isEditMode) "Changes saved successfully!" else "Your registration has been successfully completed!"
)
```

### 4. SPHome.kt
```kotlin
// Enhanced result handling
DisposableEffect(navController) {
    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.route == SERVICE_PROVIDER_REGISTRATION_HOME_SCRN) {
            val currentEntry = navController.currentBackStackEntry
            
            // Handle submission results
            val submissionResult = currentEntry?.savedStateHandle?.get<Map<String, Any>>("service_submission_result")
            // ... process submission
            
            // Handle deletion results
            val deletionResult = currentEntry?.savedStateHandle?.get<Map<String, Any>>("service_deletion_result")
            // ... process deletion
        }
    }
    // ... rest of the listener setup
}
```

## User Flow

### New Service Registration
1. User clicks on a service category in SPHome
2. Navigates to ServiceProviderRegistrationScreen2
3. `isEditMode` is false (service doesn't exist)
4. Form shows "Submit" button
5. On submission, shows "Your registration has been successfully completed!"
6. Service is added to `submittedServices` list in SPHome

### Edit Existing Service
1. User clicks on an already submitted service in SPHome
2. Navigates to ServiceProviderRegistrationScreen2
3. `loadExistingService()` detects existing service, sets `isEditMode` to true
4. Form is populated with existing data
5. TopBar shows three-dots menu
6. Form shows "Save Changes" button
7. On submission, shows "Changes saved successfully!"

### Delete Service
1. In edit mode, user clicks three-dots menu in TopBar
2. Delete popup appears with "Delete Service" option
3. User clicks "Delete Service"
4. Service is deleted permanently
5. User is navigated back to SPHome
6. Service is removed from `submittedServices` list
7. Service no longer appears in the categories section

## Technical Notes

### State Management
- Uses Kotlin StateFlow for reactive state management
- Proper state cleanup to prevent memory leaks
- Navigation state is passed through SavedStateHandle

### UI Components
- Three-dots menu uses Material Icons (MoreVert)
- Popup positioning is calculated relative to the menu button
- Popup dismissal is controlled to only close on three-dots click

### Navigation
- Uses NavController.popBackStack() for proper navigation
- Results are passed through SavedStateHandle
- Proper cleanup of navigation results after processing

## Future Enhancements
1. Add confirmation dialog before deleting service
2. Implement undo functionality for deleted services
3. Add loading states during delete operations
4. Implement bulk edit/delete functionality
5. Add service history/audit trail

## Testing Considerations
1. Test edit mode detection for existing vs new services
2. Verify proper form population in edit mode
3. Test delete functionality and UI updates
4. Verify navigation state management
5. Test popup positioning and dismissal behavior