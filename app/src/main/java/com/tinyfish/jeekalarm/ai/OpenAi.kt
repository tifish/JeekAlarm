package com.tinyfish.jeekalarm.ai

import android.util.Log
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.Schedule
import java.time.LocalDateTime
import java.time.LocalTime

object OpenAi {
    suspend fun getAnswer(question: String): Schedule? {
            val systemContent = """
你现在是一个转换器，输出给程序处理的字符串，不要输出任何多余的信息：
- 输入的每一行描述的是一个闹钟时间和相关的事件，每一行输入生成一行输出结果。
- 输出格式为: y m d h M p w a
    - y 表示年份。如果是绝对年份，例如2030年，为2030；如果是相对年份，例如3年后，为+3；明年为n1；后年为n2。如果没有年份描述，为0。
    - m 表示月份。如果是相对月份，例如3个月后，则为+3；下个月为n1；下下个月为n2。如果没有月份描述，为0。
    - d 表示月份中的第几天。如果是相对天数，例如3天后，则为+3；明天为n1；后天为n2；大后天为n3。如果没有天数描述，为0。
    - h 表示小时。如果是相对小时数，例如3小时后，则为+3。如果没有小时描述，为0。
    - M 表示分钟。如果是相对分钟数，例如3分钟后，则为+3。如果没有分钟描述，为0。
    - p 表示上午1、中午2、下午3、晚上4，如果没有这些描述，为0。
    - w 表示周数。如果是相对周数，例如3周后，则为+3；下周为n1；下下周为n2。如果没有周数描述，为0。
    - a 表示星期几。1表示星期一，7表示星期日。如果没有星期几描述，为0。
- 如果闹钟包含每天、每周、每月、每年等描述，表示是循环闹钟。如果是每年，y为*；如果是每月，m为*；如果是每天，d为*；如果是每周，w为*。
- 输出时间使用24小时制。
- 没有描述的部分请输出0。
- 如果输入不是描述闹钟，请输出null
- 以下是一些例子：
    - 我就不是闹钟: null
    - 2033年上珠峰: 2033 0 0 0 0 0 0 0
    - 两小时五分后拿外卖: 0 0 0 +2 +5 0 0 0
    - 明天中午骑车: 0 0 n1 0 0 2 0 0
    - 下周三晚上6点踢球: 0 0 0 18 0 4 n1 3
    - 一周后去医院: 0 0 0 0 0 0 +1 0
    - 每天6点起床: 0 0 * 6 0 0 0 0
    - 每个月10号还信用卡: 0 * 10 0 0 0 0 0
    - 一周后中午请客: 0 0 0 0 0 2 +1 0
"""

            val host = if (SettingsService.openAiApiUrl.isNotBlank()) {
                OpenAIHost(SettingsService.openAiApiUrl)
            } else {
                OpenAIHost.OpenAI
            }
            val openAi = OpenAI(
                host = host,
                token = SettingsService.openAiApiKey,
                // Avoid logging Authorization headers (API key) to Logcat.
                logging = LoggingConfig(LogLevel.None),
            )
            return try {
                val completionRequest = ChatCompletionRequest(
                    model = ModelId(SettingsService.openAiApiModel),
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.System, content = systemContent
                        ), ChatMessage(
                            role = ChatRole.User, content = question
                        )
                    ),
                    // 推理（thinking）模型会先输出大段 reasoning_content 再给 content，
                    // token 太小会被推理吃光导致 content 为空，所以留足空间。
                    maxTokens = 2048,
                    temperature = 0.0,
                )

                val chatCompletions = openAi.chatCompletion(completionRequest)
                val choice = chatCompletions.choices.firstOrNull()
                    ?: throw IllegalStateException("AI returned no choices")
                val result = choice.message.content.orEmpty().trim()
                if (result.isEmpty()) {
                    Log.e("OpenAI", "Empty content, finish_reason=${choice.finishReason}")
                    throw IllegalStateException("AI returned empty content (finish_reason=${choice.finishReason})")
                }
                parseGptResult(result)
            } finally {
                openAi.close()
            }
        }

        internal fun parseGptResult(gptResult: String, nowProvider: () -> LocalDateTime = { LocalDateTime.now() }): Schedule? {
            val trimmed = gptResult.trim()
            if (trimmed == "null") {
                return null
            }

            // y m d h M p w a
            val parts = trimmed.trimStart('@').split('-', ':', ' ').filter { it.isNotEmpty() }
            if (parts.size != 8)
                return null

            val yearPart = parsePart(parts[0]) ?: return null
            val monthPart = parsePart(parts[1]) ?: return null
            val dayPart = parsePart(parts[2]) ?: return null
            val hourPart = parsePart(parts[3]) ?: return null
            val minutePart = parsePart(parts[4]) ?: return null
            val periodPart = parsePart(parts[5]) ?: return null
            val weekPart = parsePart(parts[6]) ?: return null
            val weekDayPart = parsePart(parts[7]) ?: return null

            // Set default time or normalize 12-hour afternoon/evening hours.
            if (!hourPart.isPlus && !minutePart.isPlus && hourPart.value == 0 && minutePart.value == 0) {
                when (periodPart.value) {
                    0 -> hourPart.value = 12
                    1 -> hourPart.value = 9  // Morning
                    2 -> hourPart.value = 12 // Noon
                    3 -> hourPart.value = 14 // Afternoon
                    4 -> hourPart.value = 18 // Night
                }
            } else if (!hourPart.isPlus && periodPart.value in 3..4 && hourPart.value in 1..11) {
                hourPart.value += 12
            }

            // Loop
            if (yearPart.isLoop || monthPart.isLoop || dayPart.isLoop || weekPart.isLoop) {
                if (weekPart.isLoop)
                    dayPart.isLoop = true
                if (dayPart.isLoop)
                    monthPart.isLoop = true
                if (monthPart.isLoop)
                    yearPart.isLoop = true

                return Schedule(
                    yearConfig = "*",
                    monthConfig = if (monthPart.isLoop) "*" else monthPart.value.coerceAtLeast(1).toString(),
                    dayConfig = if (dayPart.isLoop) "*" else dayPart.value.coerceAtLeast(1).toString(),
                    hourConfig = hourPart.value.toString(),
                    minuteConfig = minutePart.value.toString(),
                    // AI uses 0 for an unspecified weekday; cron uses 0 for Sunday.
                    weekDayConfig = if (weekDayPart.isLoop || weekDayPart.value == 0) "*" else weekDayPart.value.toString(),
                    onlyOnce = false,
                )
            }

            // Plus
            if (yearPart.isPlus || monthPart.isPlus || dayPart.isPlus || hourPart.isPlus || minutePart.isPlus || weekPart.isPlus) {
                var now = nowProvider()
                if (yearPart.isPlus)
                    now = now.plusYears(yearPart.value.toLong())
                else if (yearPart.value > 0)
                    now = now.withYear(yearPart.value)
                if (monthPart.isPlus)
                    now = now.plusMonths(monthPart.value.toLong())
                else if (monthPart.value > 0)
                    now = now.withMonth(monthPart.value)
                if (dayPart.isPlus)
                    now = now.plusDays(dayPart.value.toLong())
                else if (dayPart.value > 0)
                    now = now.withDayOfMonth(dayPart.value)
                if (hourPart.isPlus)
                    now = now.plusHours(hourPart.value.toLong())
                else if (hourPart.value > 0)
                    now = now.withHour(hourPart.value)
                if (minutePart.isPlus)
                    now = now.plusMinutes(minutePart.value.toLong())
                else if (minutePart.value > 0 || (!hourPart.isPlus && hourPart.value > 0))
                    now = now.withMinute(minutePart.value)
                if (weekPart.isPlus)
                    now = now.plusDays((weekPart.value * 7).toLong())

                return Schedule(
                    yearConfig = now.year.toString(),
                    monthConfig = now.monthValue.toString(),
                    dayConfig = now.dayOfMonth.toString(),
                    hourConfig = now.hour.toString(),
                    minuteConfig = now.minute.toString(),
                    weekDayConfig = "*",
                    onlyOnce = true,
                )
            }

            // Next
            if (yearPart.isNext || monthPart.isNext || dayPart.isNext || weekPart.isNext || weekDayPart.value > 0) {
                var now = nowProvider()
                if (yearPart.isNext) {
                    now = now.plusYears(yearPart.value.toLong())
                    yearPart.value = now.year

                    if (monthPart.value == 0)
                        monthPart.value = 1
                    if (dayPart.value == 0)
                        dayPart.value = 1
                } else if (monthPart.isNext) {
                    now = now.plusMonths(monthPart.value.toLong())
                    yearPart.value = now.year
                    monthPart.value = now.monthValue

                    if (dayPart.value == 0)
                        dayPart.value = 1
                } else if (dayPart.isNext) {
                    now = now.plusDays(dayPart.value.toLong())

                    yearPart.value = now.year
                    monthPart.value = now.monthValue
                    dayPart.value = now.dayOfMonth
                } else if (weekPart.isNext) {
                    val weekDayNow = now.dayOfWeek.value
                    if (weekPart.value <= 0)
                        return null
                    if (weekDayPart.value == 0)
                        weekDayPart.value = 1
                    now = now.plusDays((7 - weekDayNow + (weekPart.value - 1) * 7 + weekDayPart.value).toLong())

                    yearPart.value = now.year
                    monthPart.value = now.monthValue
                    dayPart.value = now.dayOfMonth
                } else if (weekDayPart.value > 0) {
                    val weekDayNow = now.dayOfWeek.value
                    val targetTime = LocalTime.of(hourPart.value.coerceIn(0, 23), minutePart.value.coerceIn(0, 59))
                    val daysToAdd = when {
                        weekDayPart.value > weekDayNow -> weekDayPart.value - weekDayNow
                        weekDayPart.value == weekDayNow && targetTime.isAfter(now.toLocalTime()) -> 0
                        else -> 7 - weekDayNow + weekDayPart.value
                    }
                    now = now.plusDays(daysToAdd.toLong())

                    yearPart.value = now.year
                    monthPart.value = now.monthValue
                    dayPart.value = now.dayOfMonth
                }

                return Schedule(
                    yearConfig = yearPart.value.toString(),
                    monthConfig = monthPart.value.toString(),
                    dayConfig = dayPart.value.toString(),
                    hourConfig = hourPart.value.toString(),
                    minuteConfig = minutePart.value.toString(),
                    weekDayConfig = if (weekDayPart.value == 0) "*" else weekDayPart.value.toString(),
                    onlyOnce = true,
                )
            } else {
                return Schedule(
                    yearConfig = if (yearPart.value == 0) "*" else yearPart.value.toString(),
                    monthConfig = if (monthPart.value == 0) "*" else monthPart.value.toString(),
                    dayConfig = if (dayPart.value == 0) "*" else dayPart.value.toString(),
                    hourConfig = hourPart.value.toString(),
                    minuteConfig = minutePart.value.toString(),
                    weekDayConfig = if (weekDayPart.value == 0) "*" else weekDayPart.value.toString(),
                    onlyOnce = true,
                )
            }
        }

        class PartResult(
            var isLoop: Boolean = false,
            var isPlus: Boolean = false,
            var isNext: Boolean = false,
            var value: Int = 0,
        )

        private fun parsePart(part: String): PartResult? {
            val result = PartResult()
            if (part.startsWith("+")) {
                result.isPlus = true
                result.value = part.substring(1).toIntOrNull() ?: return null
            } else if (part.startsWith("n")) {
                result.isNext = true
                result.value = part.substring(1).toIntOrNull() ?: return null
            } else if (part == "*") {
                result.isLoop = true
            } else {
                result.value = part.toIntOrNull() ?: return null
            }
            return result
        }
}
