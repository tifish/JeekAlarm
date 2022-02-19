package com.tinyfish.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import kotlin.reflect.KMutableProperty0

@Composable
fun HeightSpacer(height: Dp = 10.dp) {
    Spacer(Modifier.height(height))
}

@Composable
fun WidthSpacer(width: Dp = 5.dp) {
    Spacer(Modifier.width(width))
}

@Composable
fun ToolButtonWidthSpacer() {
    WidthSpacer(36.dp)
}

@Composable
fun MyFileSelector(hint: String, text: String, onSelect: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(hint, Modifier.clickable { onSelect() })
        WidthSpacer()
        Text(text, Modifier.weight(1f, true))

        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_location_searching),
            text = "Select",
            onClick = onSelect,
        )
        WidthSpacer()
        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_clear),
            "Clear",
            onClick = onClear,
        )
    }
}

@Composable
fun MySwitch(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle = TextStyle.Default,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleSwitch(
        hint = hint,
        booleanProp = booleanProp,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
fun MySwitch(
    hint: String,
    value: Boolean,
    textStyle: TextStyle = TextStyle.Default,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleSwitch(
        hint = hint,
        value = value,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
fun MyTextButton(
    text: String = "",
    onClick: () -> Unit
) {
    SimpleTextButton(
        text = text,
        width = 30.dp,
        height = 30.dp,
        shape = CircleShape,
        onClick = onClick
    )
}

@Composable
fun MyTopBar(title: @Composable () -> Unit) {
    TopAppBar(
        title = title,
        backgroundColor = MaterialTheme.colors.primary
    )
}

@Composable
fun MyTopBar(@DrawableRes iconID: Int, title: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ImageVector.vectorResource(iconID), title)
                WidthSpacer()
                Text(title)
            }
        },
        backgroundColor = MaterialTheme.colors.primary,
    )
}

@Composable
fun MyBottomBar(buttons: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        elevation = 2.dp,
        color = MaterialTheme.colors.primary,
    ) {
        Row(Modifier.height(80.dp), Arrangement.Center, Alignment.CenterVertically) {
            buttons()
        }
    }
}

@Composable
fun MyGroupBox(modifier: Modifier = Modifier, body: @Composable () -> Unit = {}) {
    Card(
        modifier
            .fillMaxWidth()
            .padding(5.dp),
        backgroundColor = MaterialTheme.colors.primaryVariant
    ) {
        Column(Modifier.padding(10.dp)) {
            body()
        }
    }
}
