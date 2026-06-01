package com.tinyfish.ui

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R

@Composable
fun HeightSpacer(height: Dp = 10.dp) {
    Spacer(Modifier.height(height))
}

@Composable
fun WidthSpacer(width: Dp = 5.dp) {
    Spacer(Modifier.width(width))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(@DrawableRes iconID: Int, title: String, onBack: (() -> Unit)? = null) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_back), "Back")
                }
            } else {
                Icon(
                    ImageVector.vectorResource(iconID),
                    null,
                    Modifier.padding(start = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

/** 带可选标题/图标的分组卡片，子项之间自动留白。 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    @DrawableRes icon: Int? = null,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            ImageVector.vectorResource(icon),
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        WidthSpacer(8.dp)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            content()
        }
    }
}

/** 标题在左、开关在右的设置行，整行可点。 */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 文件/文件夹选择行：标题 + 当前值 + 选择/清除按钮。 */
@Composable
fun MyFileSelector(
    label: String,
    value: String,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val display = if (value.isEmpty())
                "Not set"
            else
                Uri.decode(value.substringAfterLast('/'))

            Text(
                display,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onSelect) {
                Icon(ImageVector.vectorResource(R.drawable.ic_location_searching), "Select")
            }
            IconButton(onClick = onClear) {
                Icon(ImageVector.vectorResource(R.drawable.ic_clear), "Clear")
            }
        }
    }
}
