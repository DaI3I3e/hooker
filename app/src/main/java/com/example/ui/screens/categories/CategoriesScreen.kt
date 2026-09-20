package com.example.ui.screens.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor

@Composable
fun CategoriesScreenContent(
    viewModel: CategoriesViewModel,
    onNavigateToAddCategory: (String) -> Unit,
    onNavigateToEditCategory: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) } // 0: Expense, 1: Income
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val currentType = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
    val filteredCategories = allCategories
        .filter { it.type == currentType || it.type == CategoryType.BOTH }
        .sortedWith(compareByDescending<CategoryEntity> { it.isFavorite }.thenBy { it.name })

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("حذف دسته‌بندی", fontWeight = FontWeight.Bold) },
            text = {
                Text("آیا از حذف دسته‌بندی «${categoryToDelete?.name}» اطمینان دارید؟ تراکنش‌های مرتبط حذف نخواهند شد.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCategory(it) }
                        categoryToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "دسته‌های هزینه",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) ExpenseColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "دسته‌های درآمد",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) IncomeColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ دسته‌ای یافت نشد.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        CategoryGridCard(
                            category = cat,
                            onClick = { onNavigateToEditCategory(cat.id, cat.type.name) },
                            onLongClick = { categoryToDelete = cat },
                            onToggleFavorite = { viewModel.toggleFavorite(cat) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onNavigateToAddCategory(if (selectedTab == 0) "EXPENSE" else "INCOME") },
            containerColor = if (selectedTab == 0) ExpenseColor else IncomeColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن دسته", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryGridCard(
    category: CategoryEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = Color(category.color)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = categoryColor.copy(alpha = 0.12f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (category.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (category.isFavorite) "حذف از علاقه‌مندی" else "افزودن به علاقه‌مندی",
                    tint = if (category.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.icon),
                        contentDescription = category.name,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateToAddCategory: (String) -> Unit,
    onNavigateToEditCategory: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    CategoriesScreenContent(
        viewModel = viewModel,
        onNavigateToAddCategory = onNavigateToAddCategory,
        onNavigateToEditCategory = onNavigateToEditCategory,
        modifier = modifier
    )
}
