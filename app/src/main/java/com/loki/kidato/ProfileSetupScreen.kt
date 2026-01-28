package com.loki.kidato

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    schools: List<School>,
    courses: List<Course>,
    onSchoolSelected: (schoolId: String) -> Unit,
    onSave: (
        name: String,
        regNo: String,
        schoolId: String,
        courseId: String,
        year: Int,
        semester: Int,
        semesterKey: String,
        profileCompleted: Boolean
    ) -> Unit,
    onSkip: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }

    var schoolId by remember { mutableStateOf("") }
    var courseId by remember { mutableStateOf("") }

    var yearText by remember { mutableStateOf("") }
    var semesterText by remember { mutableStateOf("") }

    var schoolExpanded by remember { mutableStateOf(false) }
    var courseExpanded by remember { mutableStateOf(false) }

    val year = yearText.toIntOrNull() ?: 0
    val semester = semesterText.toIntOrNull() ?: 0
    val semesterKey = if (year > 0 && semester > 0) "$year.$semester" else ""

    val selectedSchoolName = schools.firstOrNull { it.id == schoolId }?.name ?: "Select school"
    val selectedCourseName = courses.firstOrNull { it.id == courseId }?.name ?: "Select course"

    val profileCompleted =
        name.trim().isNotBlank() &&
                regNo.trim().isNotBlank() &&
                schoolId.isNotBlank() &&
                courseId.isNotBlank() &&
                year > 0 &&
                semester > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Complete your profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = regNo,
            onValueChange = { regNo = it },
            label = { Text("Registration Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // School dropdown
        ExposedDropdownMenuBox(
            expanded = schoolExpanded,
            onExpandedChange = { schoolExpanded = !schoolExpanded }
        ) {
            OutlinedTextField(
                value = selectedSchoolName,
                onValueChange = {},
                readOnly = true,
                label = { Text("School / Faculty") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = schoolExpanded,
                onDismissRequest = { schoolExpanded = false }
            ) {
                schools.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.name) },
                        onClick = {
                            schoolExpanded = false

                            // reset course when school changes
                            schoolId = s.id
                            courseId = ""

                            onSchoolSelected(s.id)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Course dropdown
        ExposedDropdownMenuBox(
            expanded = courseExpanded,
            onExpandedChange = { if (schoolId.isNotBlank()) courseExpanded = !courseExpanded }
        ) {
            OutlinedTextField(
                value = if (schoolId.isBlank()) "Select school first" else selectedCourseName,
                onValueChange = {},
                readOnly = true,
                enabled = schoolId.isNotBlank(),
                label = { Text("Course") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = courseExpanded,
                onDismissRequest = { courseExpanded = false }
            ) {
                courses.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name) },
                        onClick = {
                            courseExpanded = false
                            courseId = c.id
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = yearText,
                onValueChange = { yearText = it.filter(Char::isDigit).take(1) },
                label = { Text("Year") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = semesterText,
                onValueChange = { semesterText = it.filter(Char::isDigit).take(1) },
                label = { Text("Semester") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        if (semesterKey.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Semester: $semesterKey")
        }

        Spacer(Modifier.height(18.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = profileCompleted,
            onClick = {
                onSave(
                    name.trim(),
                    regNo.trim(),
                    schoolId,
                    courseId,
                    year,
                    semester,
                    semesterKey,
                    profileCompleted
                )
            }
        ) {
            Text("Save Profile")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSkip
        ) {
            Text("Skip for now")
        }

        if (!profileCompleted) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Fill name, reg no, school, course, year, semester.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
