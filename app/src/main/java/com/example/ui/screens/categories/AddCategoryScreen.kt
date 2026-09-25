package com.example.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryType
import com.example.ui.components.AppColorPalette
import com.example.ui.components.AppIconsList
import com.example.ui.components.getCategoryIcon

val CategoryIconsList = AppIconsList
val CategoryColorPalette = AppColorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    viewModel: AddCategoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "ویرایش دسته‌بندی" else "افزودن دسته‌بندی جدید",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Name
            Text("نام دسته‌بندی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.setName(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("مثال: رستوران، سوپرمارکت") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Type
            Text("نوع دسته‌بندی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.type == CategoryType.EXPENSE,
                    onClick = { viewModel.setType(CategoryType.EXPENSE) },
                    label = { Text("هزینه") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.type == CategoryType.INCOME,
                    onClick = { viewModel.setType(CategoryType.INCOME) },
                    label = { Text("درآمد") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.type == CategoryType.BOTH,
                    onClick = { viewModel.setType(CategoryType.BOTH) },
                    label = { Text("هر دو") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Monthly Budget (for Expense/Both categories)
            if (state.type != CategoryType.INCOME) {
                Text("سقف بودجه ماهانه (اختیاری)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = if (state.monthlyBudget > 0) (state.monthlyBudget / 10).toString() else "",
                    onValueChange = { str ->
                        val toman = str.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        viewModel.setMonthlyBudget(toman * 10)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("سقف مجاز خرج‌کرد ماهانه (تومان)") },
                    placeholder = { Text("مثال: ۲،۰۰۰،۰۰۰") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Icon Picker
            Text("انتخاب آیکون", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CategoryIconsList) { iconName ->
                    val isSelected = state.icon == iconName
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(state.color).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color(state.color) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setIcon(iconName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(iconName),
                            contentDescription = iconName,
                            tint = if (isSelected) Color(state.color) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Color Picker
            Text("رنگ دسته‌بندی (۲۴+ رنگ)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryColorPalette.forEach { color ->
                    val colorArgb = color.toArgb()
                    val isSelected = state.color == colorArgb

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setColor(colorArgb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "انتخاب شده",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveCategory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(state.color))
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "ذخیره")
                Spacer(modifier = Modifier.width(8.dp))
                Text("ذخیره دسته‌بندی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
