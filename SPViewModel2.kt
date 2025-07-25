import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SPViewModel2 : ViewModel() {
    
    // Existing properties (add these based on your current implementation)
    private val _submissionSuccess = MutableStateFlow(false)
    val submissionSuccess: StateFlow<Boolean> = _submissionSuccess.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _showValidationErrors = MutableStateFlow(false)
    val showValidationErrors: StateFlow<Boolean> = _showValidationErrors.asStateFlow()
    
    private val _chargesValidationError = MutableStateFlow<String?>(null)
    val chargesValidationError: StateFlow<String?> = _chargesValidationError.asStateFlow()
    
    private val _selectedWorkstyle = MutableStateFlow<List<String>>(emptyList())
    val selectedWorkstyle: StateFlow<List<String>> = _selectedWorkstyle.asStateFlow()
    
    private val _selectedOptions = MutableStateFlow<List<String>>(emptyList())
    val selectedOptions: StateFlow<List<String>> = _selectedOptions.asStateFlow()
    
    private val _selectedTimeSlots = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedTimeSlots: StateFlow<Map<String, String>> = _selectedTimeSlots.asStateFlow()
    
    private val _isSubmittedSuccessfully = MutableStateFlow(false)
    val isSubmittedSuccessfully: StateFlow<Boolean> = _isSubmittedSuccessfully.asStateFlow()
    
    // New properties for edit mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    private val _deleteServiceSuccess = MutableStateFlow(false)
    val deleteServiceSuccess: StateFlow<Boolean> = _deleteServiceSuccess.asStateFlow()
    
    // Properties for validation and workstyle
    val availableWorkstyle = listOf("On-site", "Remote", "Hybrid")
    var workstyleErrorMessage = ""
    var isWorkstyleValid = true
    
    // Methods for edit mode
    fun setEditMode(isEditing: Boolean) {
        _isEditMode.value = isEditing
    }
    
    fun loadExistingService(serviceName: String) {
        // Check if service already exists
        val serviceExists = checkIfServiceExists(serviceName)
        _isEditMode.value = serviceExists
        
        if (serviceExists) {
            // Load existing service data
            loadServiceData(serviceName)
        }
    }
    
    private fun checkIfServiceExists(serviceName: String): Boolean {
        // Implement your logic to check if service exists
        // This could be checking local database, shared preferences, or API
        // For now, returning false - replace with actual implementation
        return false
    }
    
    private fun loadServiceData(serviceName: String) {
        // Load existing service data and populate the form fields
        // Implement based on your data source
    }
    
    fun deleteService(serviceName: String) {
        viewModelScope.launch {
            try {
                // Implement delete service logic here
                // This could involve API call, database deletion, etc.
                
                // For now, just set success
                _deleteServiceSuccess.value = true
                
            } catch (e: Exception) {
                // Handle error
                _errorMessage.value = "Failed to delete service: ${e.message}"
            }
        }
    }
    
    fun resetDeleteServiceSuccess() {
        _deleteServiceSuccess.value = false
    }
    
    // Existing methods (implement based on your current ViewModel)
    fun onSubmitForm2() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Your form submission logic here
                
                _submissionSuccess.value = true
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }
    
    fun setSubmissionSuccess(success: Boolean) {
        _submissionSuccess.value = success
    }
    
    fun setSubmitClicked(clicked: Boolean) {
        // Implement if needed
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun onSpecializationChanged(updatedList: List<String>) {
        // Implement specialization change logic
    }
    
    fun toggleWorkstyleSelection(workstyle: String) {
        val currentList = _selectedWorkstyle.value.toMutableList()
        if (currentList.contains(workstyle)) {
            currentList.remove(workstyle)
        } else {
            currentList.add(workstyle)
        }
        _selectedWorkstyle.value = currentList
    }
    
    fun validateWorkstyle() {
        isWorkstyleValid = _selectedWorkstyle.value.isNotEmpty()
        workstyleErrorMessage = if (!isWorkstyleValid) "Please select at least one workstyle" else ""
    }
    
    fun toggleOption(option: String) {
        val currentList = _selectedOptions.value.toMutableList()
        if (currentList.contains(option)) {
            currentList.remove(option)
        } else {
            currentList.add(option)
        }
        _selectedOptions.value = currentList
    }
    
    fun toggleTimeSlot(option: String, slot: String) {
        val currentSlots = _selectedTimeSlots.value.toMutableMap()
        if (currentSlots[option] == slot) {
            currentSlots.remove(option)
        } else {
            currentSlots[option] = slot
        }
        _selectedTimeSlots.value = currentSlots
    }
}

data class UiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)