package com.tinyfish.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(title: @Composable () -> Unit) {
    TopAppBar(
        title = title,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    )
}

@Composable
fun MyBottomBar(buttons: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth()) {
        Row(Modifier.height(60.dp), Arrangement.Center, Alignment.CenterVertically) {
            buttons()
        }
    }
}

@Composable
fun MyGroupBox(modifier: Modifier = Modifier, body: @Composable () -> Unit = {}) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            body()
        }
    }
}
