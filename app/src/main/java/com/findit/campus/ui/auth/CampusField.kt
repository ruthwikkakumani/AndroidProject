package com.findit.campus.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import com.findit.campus.auth.campusSuggestions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val names = stringArrayResource(R.array.campus_names).toList()
    val suggestions = remember(value, names) { campusSuggestions(value, names) }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && suggestions.isNotEmpty(), { expanded = it }, Modifier.fillMaxWidth()) {
        OutlinedTextField(value, { onChange(it.take(80)); expanded = true },
            modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            label = { Text("College / campus") }, singleLine = true, shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && suggestions.isNotEmpty()) },
            supportingText = { Text("Choose a suggestion, or type your own college.") })
        ExposedDropdownMenu(expanded && suggestions.isNotEmpty(), { expanded = false }, Modifier.heightIn(max = 240.dp)) {
            suggestions.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = {
                onChange(name); expanded = false
            }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding) }
        }
    }
}
