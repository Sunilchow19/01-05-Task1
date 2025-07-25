data class ServiceProviderUiState(
    val isLoading: Boolean = false,
    val expectedCharge: String = "50-100",
    val expectedCharges: String = "",
    val specializations: List<SpecializationWithCharge> = emptyList(),
    val errorMessage: String? = null,
    val selectedTimeSlots: Map<String, List<String>> = emptyMap(),
    val specializationsWithCharges: List<SpecializationWithCharge> = emptyList(),
    val selectedworkstyle: List<String> = emptyList(),
    val serviceProviderId: String = "",
    val selectedOptions: List<String> = emptyList(),
)

class SPViewModel2(
    private val firebaseRepository: FirebaseRepositorys,
    private val context: Context
) : ViewModel() {

    private val _submitClicked = MutableStateFlow(false)
    val submitClicked: StateFlow<Boolean> = _submitClicked

    fun setSubmitClicked(clicked: Boolean) {
        _submitClicked.value = clicked
    }

    private val _showValidationErrors = MutableStateFlow(false)
    val showValidationErrors: StateFlow<Boolean> = _showValidationErrors

    fun setShowValidationErrors(show: Boolean) {
        _showValidationErrors.value = show
    }

    private val _specializationState = MutableStateFlow<List<SpecializationWithCharge>>(emptyList())
    val specializationState: StateFlow<List<SpecializationWithCharge>> = _specializationState

    val isSubmittedSuccessfully = mutableStateOf(false)
    val submissionSuccess = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    private val _selectedOptions = MutableStateFlow<List<String>>(emptyList())
    val selectedOptions: StateFlow<List<String>> get() = _selectedOptions

    private val _selectedTimeSlots = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val selectedTimeSlots: StateFlow<Map<String, List<String>>> get() = _selectedTimeSlots

    private val _uiState = MutableStateFlow(ServiceProviderUiState())
    val uiState: StateFlow<ServiceProviderUiState> get() = _uiState

    private val _chargesValidationError = MutableStateFlow<String?>(null)
    val chargesValidationError: StateFlow<String?> get() = _chargesValidationError

    private val _timeSlotValidationError = MutableStateFlow<String?>(null)
    val timeSlotValidationError: StateFlow<String?> get() = _timeSlotValidationError

    private val _selectedSpecializations = mutableStateListOf<String>()
    val selectedSpecializations: List<String> get() = _selectedSpecializations

    private val _chargesPerSpecialization = mutableStateMapOf<String, Charges>()
    val chargesPerSpecialization: Map<String, Charges> get() = _chargesPerSpecialization

    private val _experiencePerSpecialization = mutableStateMapOf<String, String>()
    val experiencePerSpecialization: Map<String, String> get() = _experiencePerSpecialization

    private val _selectedSubSpecializations = mutableStateMapOf<String, List<String>>()
    val selectedSubSpecializations: Map<String, List<String>> get() = _selectedSubSpecializations

    private val _showSpecializationErrorAfterSubmit = mutableStateOf(false)
    val showSpecializationErrorAfterSubmit: State<Boolean> get() = _showSpecializationErrorAfterSubmit

    private val _selectedWorkstyle = mutableStateOf(listOf<String>())
    val selectedWorkstyle: State<List<String>> get() = _selectedWorkstyle

    val availableWorkstyle = listOf("Hour", "Day", "Week", "Month")
    var workstyleErrorMessage by mutableStateOf("")
        private set

    var isWorkstyleValid by mutableStateOf(false)
        private set

    var showValidationsErrors by mutableStateOf(false)
        private set

    val showSpecializationError = mutableStateOf(false)

    // ===== Update Functions =====

    fun updateSelectedOptions(options: List<String>) {
        _selectedOptions.value = options
        _uiState.value = _uiState.value.copy(selectedOptions = options)
    }

    fun toggleWorkstyleSelection(workstyle: String) {
        _selectedWorkstyle.value = if (_selectedWorkstyle.value.contains(workstyle)) {
            _selectedWorkstyle.value - workstyle
        } else {
            _selectedWorkstyle.value + workstyle
        }
        _uiState.value = _uiState.value.copy(selectedworkstyle = _selectedWorkstyle.value)
    }

    // Update validateWorkstyle function:
    fun validateWorkstyle(): Boolean {
        isWorkstyleValid = _selectedWorkstyle.value.isNotEmpty()
        workstyleErrorMessage = if (isWorkstyleValid) "" else "Please select at least one work style"
        return isWorkstyleValid
    }

    fun toggleTimeSlot(option: String, slot: String) {
        _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableMap().apply {
            val currentSlots = this[option]?.toMutableList() ?: mutableListOf()
            if (currentSlots.contains(slot)) {
                currentSlots.remove(slot)
            } else {
                currentSlots.add(slot)
            }
            this[option] = currentSlots
        }

        // Update active options here after the state is updated
        val activeOptions = _selectedTimeSlots.value.filter { it.value.isNotEmpty() }.keys.toList()
        _selectedOptions.value = activeOptions

        _uiState.value = _uiState.value.copy(
            selectedOptions = activeOptions,
            selectedTimeSlots = _selectedTimeSlots.value
        )
    }

    fun addSpecialization(category: String) {
        if (!_selectedSpecializations.contains(category)) {
            _selectedSpecializations.add(category)
            _chargesPerSpecialization.putIfAbsent(category, Charges())
            _experiencePerSpecialization.putIfAbsent(category, "")
            updateSpecializationState()
        }
    }

    fun removeSpecialization(category: String) {
        _selectedSpecializations.remove(category)
        _chargesPerSpecialization.remove(category)
        _experiencePerSpecialization.remove(category)
        _selectedSubSpecializations.remove(category)
        updateSpecializationState()
        
        // Also remove from Firebase
        viewModelScope.launch {
            firebaseRepository.deleteSpecialization(category)
        }
    }

    fun updateCharges(category: String, charges: Charges) {
        _chargesPerSpecialization[category] = charges
        updateSpecializationState()
        
        // Update specific specialization in Firebase
        viewModelScope.launch {
            val subSpecs = _selectedSubSpecializations[category] ?: emptyList()
            firebaseRepository.updateSpecificSpecialization(
                specializationName = category,
                charges = charges.toMap(),
                subSpecializations = subSpecs
            ) { success, error ->
                if (!success) {
                    Log.e("SPViewModel2", "Failed to update charges for $category: $error")
                }
            }
        }
    }

    fun updateExperience(category: String, experience: String) {
        _experiencePerSpecialization[category] = experience
        updateSpecializationState()
    }

    fun addSubSpecialization(category: String, subSpecialization: String) {
        val current = _selectedSubSpecializations[category]?.toMutableList() ?: mutableListOf()
        if (!current.contains(subSpecialization)) {
            current.add(subSpecialization)
            _selectedSubSpecializations[category] = current
            updateSpecializationState()
            
            // Update specific specialization in Firebase
            viewModelScope.launch {
                val charges = _chargesPerSpecialization[category]?.toMap() ?: emptyMap()
                firebaseRepository.updateSpecificSpecialization(
                    specializationName = category,
                    charges = charges,
                    subSpecializations = current
                ) { success, error ->
                    if (!success) {
                        Log.e("SPViewModel2", "Failed to update subspecializations for $category: $error")
                    }
                }
            }
        }
    }

    fun onSpecializationChanged(newSpecializations: List<SpecializationWithCharge>) {
        _selectedSpecializations.clear()
        _selectedSpecializations.addAll(newSpecializations.map { it.specialization })

        // Keep existing charges and experience if still valid
        val filteredCharges = _chargesPerSpecialization.filterKeys { key ->
            newSpecializations.any { it.specialization == key }
        }
        _chargesPerSpecialization.clear()
        _chargesPerSpecialization.putAll(filteredCharges)

        val filteredExperience = _experiencePerSpecialization.filterKeys { key ->
            newSpecializations.any { it.specialization == key }
        }
        _experiencePerSpecialization.clear()
        _experiencePerSpecialization.putAll(filteredExperience)

        val filteredSubSpecs = _selectedSubSpecializations.filterKeys { key ->
            newSpecializations.any { it.specialization == key }
        }
        _selectedSubSpecializations.clear()
        _selectedSubSpecializations.putAll(filteredSubSpecs)

        _specializationState.value = newSpecializations
        _uiState.value = _uiState.value.copy(specializationsWithCharges = newSpecializations)

        updateSpecializationState()
    }

    fun removeSubSpecialization(category: String, subSpecialization: String) {
        val current = _selectedSubSpecializations[category]?.toMutableList() ?: mutableListOf()
        current.remove(subSpecialization)
        _selectedSubSpecializations[category] = current
        updateSpecializationState()
        
        // Update specific specialization in Firebase
        viewModelScope.launch {
            val charges = _chargesPerSpecialization[category]?.toMap() ?: emptyMap()
            firebaseRepository.updateSpecificSpecialization(
                specializationName = category,
                charges = charges,
                subSpecializations = current
            ) { success, error ->
                if (!success) {
                    Log.e("SPViewModel2", "Failed to update subspecializations for $category: $error")
                }
            }
        }
    }

    private fun updateSpecializationState() {
        val specializations = _selectedSpecializations.map { spec ->
            SpecializationWithCharge(
                specialization = spec,
                subSpecializations = _selectedSubSpecializations[spec] ?: emptyList(),
                charges = _chargesPerSpecialization[spec],
            )
        }
        _specializationState.value = specializations
        _uiState.value = _uiState.value.copy(specializationsWithCharges = specializations)
    }

    // ===== Validation Functions =====

    fun validateSpecializationsOnSubmit(): Boolean {
        val isValid = isSpecializationDataValid(_specializationState.value)
        _showSpecializationErrorAfterSubmit.value = !isValid
        return isValid
    }

    private fun validateTimeSlots(
        selectedOptions: List<String>,
        selectedTimeSlots: Map<String, List<String>>
    ): Boolean {
        if (selectedOptions.isEmpty()) {
            _timeSlotValidationError.value = "Please select at least one option"
            return false
        }

        for (option in selectedOptions) {
            if (selectedTimeSlots[option].isNullOrEmpty()) {
                _timeSlotValidationError.value = "Please select time slots for all selected options"
                return false
            }
        }

        _timeSlotValidationError.value = null
        return true
    }

    private fun validateCharges(specializationsWithCharges: List<SpecializationWithCharge>): Boolean {
        _chargesValidationError.value = null

        for (specialization in specializationsWithCharges) {
            val charges = specialization.charges
            if (charges != null) {
                val isFilled = charges.perHour.isNotEmpty() ||
                        charges.perDay.isNotEmpty() ||
                        charges.perWeek.isNotEmpty() ||
                        charges.perMonth.isNotEmpty()

                if (!isFilled) {
                    _chargesValidationError.value = "At least one service charge must be entered for each specialization."
                    return false
                }

                if (charges.perHour.isNotEmpty() && (charges.perHour.toIntOrNull() ?: 0) < 100) {
                    _chargesValidationError.value = "Minimum charge for Per Hour is ₹100"
                    return false
                }
                if (charges.perDay.isNotEmpty() && (charges.perDay.toIntOrNull() ?: 0) < 100) {
                    _chargesValidationError.value = "Minimum charge for Per Day is ₹100"
                    return false
                }
                if (charges.perWeek.isNotEmpty() && (charges.perWeek.toIntOrNull() ?: 0) < 1000) {
                    _chargesValidationError.value = "Minimum charge for Per Week is ₹1000"
                    return false
                }
                if (charges.perMonth.isNotEmpty() && (charges.perMonth.toIntOrNull() ?: 0) < 1000) {
                    _chargesValidationError.value = "Minimum charge for Per Month is ₹1000"
                    return false
                }
            } else {
                _chargesValidationError.value = "Please enter charges for all specializations"
                return false
            }
        }

        return true
    }

    fun isSpecializationDataValid(specializations: List<SpecializationWithCharge>): Boolean {
        if (specializations.isEmpty()) {
            showSpecializationError.value = true
            return false
        }

        val isValid = specializations.all {
            val hasSubSpecs = it.subSpecializations.isNotEmpty()
            val hasCharges = it.charges?.let { c ->
                c.perHour.isNotEmpty() || c.perDay.isNotEmpty() || c.perWeek.isNotEmpty() || c.perMonth.isNotEmpty()
            } ?: false
            hasSubSpecs && hasCharges
        }

        showSpecializationError.value = !isValid
        return isValid
    }

    fun enableValidationErrors() {
        showValidationsErrors = true
    }

    var experienceInput by mutableStateOf("")
        private set

    fun updateExperienceInput(newValue: String) {
        if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
            experienceInput = newValue
        }
    }

    // New function to load all specializations for the user
    fun loadAllUserSpecializations() = viewModelScope.launch {
        try {
            Log.d("LOAD_DEBUG", "Loading all user specializations")
            val allSpecializations = firebaseRepository.getAllSpecializationsForUser()
            
            if (allSpecializations != null) {
                Log.d("LOAD_DEBUG", "Found specializations: ${allSpecializations.keys}")
                
                // Clear existing data
                _selectedSpecializations.clear()
                _chargesPerSpecialization.clear()
                _selectedSubSpecializations.clear()
                
                // Load each specialization
                val specializationsList = mutableListOf<SpecializationWithCharge>()
                
                allSpecializations.forEach { (name, data) ->
                    val chargesMap = data["charges"] as? Map<*, *>
                    val charges = Charges(
                        perHour = chargesMap?.get("perHour") as? String ?: "",
                        perDay = chargesMap?.get("perDay") as? String ?: "",
                        perWeek = chargesMap?.get("perWeek") as? String ?: "",
                        perMonth = chargesMap?.get("perMonth") as? String ?: ""
                    )
                    
                    val subSpecializations = (data["subSpecializations"] as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
                    
                    val specializationData = SpecializationWithCharge(
                        specialization = name,
                        charges = charges,
                        subSpecializations = subSpecializations
                    )
                    
                    specializationsList.add(specializationData)
                    _selectedSpecializations.add(name)
                    _chargesPerSpecialization[name] = charges
                    _selectedSubSpecializations[name] = subSpecializations
                }
                
                _specializationState.value = specializationsList
                _uiState.value = _uiState.value.copy(specializationsWithCharges = specializationsList)
                
                Log.d("LOAD_DEBUG", "Loaded ${specializationsList.size} specializations")
            } else {
                Log.d("LOAD_DEBUG", "No specializations found for user")
            }
        } catch (e: Exception) {
            Log.e("LOAD_DEBUG", "Error loading all specializations", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to load specializations")
        }
    }

    fun loadExistingService(serviceName: String) = viewModelScope.launch {
        try {
            Log.d("LOAD_DEBUG", "Starting to load existing service: $serviceName")
            val savedMap = firebaseRepository.getServiceForUser(serviceName)

            if (savedMap != null) {
                Log.d("LOAD_DEBUG", "Found existing data: $savedMap")

                // Load workstyle data FIRST
                val savedWorkstyle = savedMap["selectedWorkStyle"] as? List<*>
                if (savedWorkstyle != null) {
                    val workstyleList = savedWorkstyle.mapNotNull { it as? String }
                    Log.d("LOAD_DEBUG", "Loading workstyle: $workstyleList")
                    _selectedWorkstyle.value = workstyleList
                    Log.d("LOAD_DEBUG", "Workstyle set to: ${_selectedWorkstyle.value}")
                } else {
                    Log.d("LOAD_DEBUG", "No workstyle data found")
                }

                // Load time slots and options
                val savedTimeSlots = savedMap["selectedTimeSlots"] as? Map<*, *>
                if (savedTimeSlots != null) {
                    val timeSlots = savedTimeSlots.mapKeys { it.key as String }
                        .mapValues { (it.value as? List<*>)?.mapNotNull { slot -> slot as? String } ?: emptyList() }

                    Log.d("LOAD_DEBUG", "Loading timeSlots: $timeSlots")
                    _selectedTimeSlots.value = timeSlots

                    // Update selected options based on time slots
                    val activeOptions = timeSlots.filter { it.value.isNotEmpty() }.keys.toList()
                    Log.d("LOAD_DEBUG", "Derived activeOptions: $activeOptions")
                    _selectedOptions.value = activeOptions
                } else {
                    Log.d("LOAD_DEBUG", "No timeSlots data found")
                }

                // Also check for saved options separately
                val savedOptions = savedMap["selectedOptions"] as? List<*>
                if (savedOptions != null) {
                    val optionsList = savedOptions.mapNotNull { it as? String }
                    Log.d("LOAD_DEBUG", "Loading savedOptions: $optionsList")
                    _selectedOptions.value = optionsList
                }

                // Load specializations
                val chargesMap = savedMap["charges"] as? Map<*, *>
                val charges = Charges(
                    perHour = chargesMap?.get("perHour") as? String ?: "",
                    perDay = chargesMap?.get("perDay") as? String ?: "",
                    perWeek = chargesMap?.get("perWeek") as? String ?: "",
                    perMonth = chargesMap?.get("perMonth") as? String ?: ""
                )

                val subSpecializations = (savedMap["subSpecializations"] as? List<*>)
                    ?.mapNotNull { it as? String } ?: emptyList()

                val specializationData = SpecializationWithCharge(
                    specialization = savedMap["specialization"] as? String ?: serviceName,
                    charges = charges,
                    subSpecializations = subSpecializations
                )

                // Update specialization data
                _selectedSpecializations.clear()
                _selectedSpecializations.add(specializationData.specialization)
                _chargesPerSpecialization[specializationData.specialization] = charges
                _selectedSubSpecializations[specializationData.specialization] = subSpecializations
                _specializationState.value = listOf(specializationData)

                // Load experience
                val savedExperience = savedMap["experience"] as? String
                if (savedExperience != null) {
                    experienceInput = savedExperience
                    Log.d("LOAD_DEBUG", "Loading experience: $savedExperience")
                }

                // Update UI state with all loaded data
                _uiState.value = _uiState.value.copy(
                    specializationsWithCharges = listOf(specializationData),
                    selectedworkstyle = _selectedWorkstyle.value,
                    selectedOptions = _selectedOptions.value,
                    selectedTimeSlots = _selectedTimeSlots.value
                )

                // Log final state
                Log.d("LOAD_DEBUG", """
                Data loading completed:
                - Workstyle StateFlow: ${_selectedWorkstyle.value}
                - Options StateFlow: ${_selectedOptions.value}
                - TimeSlots StateFlow: ${_selectedTimeSlots.value}
                - UI State workstyle: ${_uiState.value.selectedworkstyle}
                - UI State options: ${_uiState.value.selectedOptions}
                - UI State timeSlots: ${_uiState.value.selectedTimeSlots}
            """.trimIndent())

            } else {
                Log.d("LOAD_DEBUG", "No existing data found for service: $serviceName")
            }
        } catch (e: Exception) {
            Log.e("LOAD_DEBUG", "Error loading existing service data", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to load existing data")
        }
    }

    // ===== SUBMISSION =====

    fun onSubmitForm2() {
        _submitClicked.value = true
        setShowValidationErrors(true)
        enableValidationErrors()

        // Sync UI state
        _uiState.value = _uiState.value.copy(
            selectedworkstyle = _selectedWorkstyle.value,
            selectedOptions = _selectedOptions.value,
            selectedTimeSlots = _selectedTimeSlots.value,
            specializationsWithCharges = _specializationState.value
        )

        Log.d(
            "ServiceProviderViewModel", """
            Form Data Before Validation:
            - selectedOptions: ${_selectedOptions.value}
            - selectedTimeSlots: ${_selectedTimeSlots.value}
            - specializations: ${_specializationState.value}
        """.trimIndent()
        )

        val workstyleValid = validateWorkstyle()
        val specializationsValid = validateSpecializationsOnSubmit()
        val timeSlotsValid = validateTimeSlots(_selectedOptions.value, _selectedTimeSlots.value)
        val chargesValid = validateCharges(_specializationState.value)

        Log.d(
            "ServiceProviderViewModel", """
            Validation Results:
            - Workstyle: $workstyleValid
            - Specializations: $specializationsValid
            - TimeSlots: $timeSlotsValid
            - Charges: $chargesValid
        """.trimIndent()
        )

        if (!workstyleValid || !specializationsValid || !timeSlotsValid || !chargesValid) {
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        val experience = experienceInput.ifBlank { "0" }

        viewModelScope.launch {
            firebaseRepository.saveServiceProviderData(
                specializationsWithCharges = _specializationState.value,
                selectedTimeSlots = _selectedTimeSlots.value,
                selectedOptions = _selectedOptions.value,
                selectedworkstyle = _selectedWorkstyle.value,
                experience = experience,
                context = context,
            ) { isSuccess, message ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (isSuccess) {
                    isSubmittedSuccessfully.value = true
                    submissionSuccess.value = true
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                } else {
                    errorMessage.value = message ?: "Failed to save data."
                    _uiState.value = _uiState.value.copy(errorMessage = message)
                    submissionSuccess.value = false
                }
            }
        }
    }

    private fun validateFields(currentState: ServiceProviderUiState): Boolean {

        Log.d("ValidationDebug", "Validating fields...")
        val isWorkstyleValid = validateWorkstyle()
        if (!isWorkstyleValid) {
            return false
        }

        val basicFieldsValid =
            currentState.selectedworkstyle.isNotEmpty() &&
                    currentState.selectedTimeSlots.isNotEmpty() &&
                    currentState.specializationsWithCharges.isNotEmpty()

        if (!basicFieldsValid) {
            Log.d("ValidationDebug", "Basic fields validation failed. Missing fields:")
            if (currentState.selectedworkstyle.isEmpty()) Log.d(
                "ValidationDebug",
                "Selected Workstyle is empty"
            )
            if (currentState.selectedTimeSlots.isEmpty()) Log.d(
                "ValidationDebug",
                "Selected Time Slots is empty"
            )
            if (currentState.specializationsWithCharges.isEmpty()) Log.d(
                "ValidationDebug",
                "Specializations are empty"
            )
            if (currentState.selectedOptions.isEmpty()) {
                _timeSlotValidationError.value = " "
                return false
            }

            // Check that each selected option has at least one time slot
            for (option in currentState.selectedOptions) {
                if (currentState.selectedTimeSlots[option].isNullOrEmpty()) {
                    _timeSlotValidationError.value = ""
                    return false
                }
            }
            Log.d("ValidationDebug", "Basic fields validation failed")
            _uiState.update { it.copy(errorMessage = "Please fill out all required fields correctly.") }

            if (!validateTimeSlots(currentState.selectedOptions, currentState.selectedTimeSlots)) {
                return false
            }

            return false
        }

        val allSpecializationsHaveSubSpecializations =
            currentState.specializationsWithCharges.all { specialization ->
                specialization.subSpecializations.isNotEmpty()
            }

        if (!allSpecializationsHaveSubSpecializations) {
            Log.d(
                "ValidationDebug",
                "All specializations must have at least one sub-specialization"
            )
            _uiState.update { it.copy(errorMessage = "Please select at least one sub-specialization for each specialization.") }
            return false
        }

        Log.d("ValidationDebug", "All fields are valid")
        return true
    }

    fun toggleOption(option: String) {
        val newSelectedOptions = if (_selectedOptions.value.contains(option)) {
            _selectedOptions.value - option
        } else {
            _selectedOptions.value + option
        }

        _selectedOptions.value = newSelectedOptions
        if (!newSelectedOptions.contains(option)) {
            _selectedTimeSlots.value = _selectedTimeSlots.value.toMutableMap().apply {
                remove(option)
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedOptions = _selectedOptions.value,
            selectedTimeSlots = _selectedTimeSlots.value
        )
    }

    fun validateAndPotentiallyClearError() {
        if (isSpecializationDataValid(_specializationState.value)) {
            showSpecializationError.value = false
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setSubmissionSuccess(success: Boolean) {
        submissionSuccess.value = true
    }

    class ExperienceViewModel : ViewModel() {

        // Holds the experience value as string (empty means optional)
        var experienceInput by mutableStateOf("")
            private set

        // Update experience input
        fun onExperienceChanged(value: String) {
            if (value.length <= 2 && value.all { it.isDigit() }) {
                experienceInput = value
            }
        }

        // Save to Firestore
        fun saveExperienceToFirestore(
            userId: String,
            onSuccess: () -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            val db = Firebase.firestore
            val experienceInt = experienceInput.toIntOrNull()

            // Only save if input is valid and not empty
            val data = if (experienceInput.isNotEmpty()) {
                mapOf("experience" to experienceInt)
            } else {
                mapOf<String, Any>() // optional field: skip it
            }

            db.collection("users").document(userId)
                .set(data, SetOptions.merge()) // Merge to avoid overwriting
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { exception -> onFailure(exception) }
        }

        // Optionally fetch from Firestore
        fun loadExperienceFromFirestore(userId: String) {
            val db = Firebase.firestore
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val value = document.getLong("experience")?.toInt()
                    experienceInput = value?.toString() ?: ""
                }
        }
    }
}