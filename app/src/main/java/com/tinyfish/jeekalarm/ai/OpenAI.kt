package com.tinyfish.jeekalarm.ai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.Schedule
import java.time.LocalDateTime

class OpenAI {
    companion object {
        @OptIn(BetaOpenAI::class)
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
- 如果闹钟包含每天、每周、每月、每年等描述，表示是循环闹钟。如果是每年，y为*；如果是每月，d为*；如果是每天，d为*；如果是每周，w为*。
- 输出时间使用24小时制。
- 没有描述的部分请输出0。
- 如果输入不是描述闹钟，请输出null
- 以下是一些例子：
    - 我就不是闹钟: null
    - 2033年上珠峰: 2033 0 0 0 0 0 0 0
    - 两小时五分后拿外卖: 0 0 0 +2 +5 0 0 0
    - 明天中午骑车: 0 0 n1 0 0 2 0 0
    - 下周三晚上6点踢球: 0 0 0 6 0 4 n1 3
    - 一周后去医院: 0 0 0 0 0 0 +1 0
    - 每天6点起床: 0 0 * 6 0 0 0 0
    - 每个月10号还信用卡: 0 * 10 0 0 0 0 0
    - 一周后中午请客: 0 0 0 0 0 2 +1 0
"""

            val openAI = OpenAI(OpenAIConfig(SettingsService.openAiApiKey, LoggingConfig(LogLevel.All)))
            val gpt35turbo = openAI.model(modelId = ModelId("gpt-4o"))
            val completionRequest = ChatCompletionRequest(
                model = gpt35turbo.id,
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System, content = systemContent
                    ), ChatMessage(
                        role = ChatRole.User, content = question
                    )
                ),
                maxTokens = 32,
                temperature = 0.0,
            )

            val result = openAI.chatCompletion(completionRequest).choices[0].message.content.orEmpty()
            return parseGptResult(result)
        }

        private fun parseGptResult(gptResult: String): Schedule? {
            if (gptResult == "null") {
                return null
            }

            // y m d h M p w a
            val parts = gptResult.trimStart('@').split('-', ':', ' ')
            if (parts.size != 8)
                return null

            val yearPart = parsePart(parts[0])
            val monthPart = parsePart(parts[1])
            val dayPart = parsePart(parts[2])
            val hourPart = parsePart(parts[3])
            val minutePart = parsePart(parts[4])
            val periodPart = parsePart(parts[5])
            val weekPart = parsePart(parts[6])
            val weekDayPart = parsePart(parts[7])

            // Set default time
            if (hourPart.value == 0 && minutePart.value == 0) {
                if (periodPart.value == 0) {
                    hourPart.value = 12
                } else if (periodPart.value == 1) { // Morning
                    hourPart.value = 9
                } else if (periodPart.value == 2) { // Noon
                    hourPart.value = 12
                } else if (periodPart.value == 3) { // Afternoon
                    hourPart.value = 14
                } else if (periodPart.value == 4) { // Night
                    hourPart.value = 18
                }
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
                    monthConfig = if (monthPart.isLoop) "*" else monthPart.value.toString(),
                    dayConfig = if (dayPart.isLoop) "*" else dayPart.value.toString(),
                    hourConfig = hourPart.value.toString(),
                    minuteConfig = minutePart.value.toString(),
                    weekDayConfig = if (weekDayPart.isLoop) "*" else weekDayPart.value.toString(),
                    onlyOnce = false,
                )
            }

            // Plus
            if (yearPart.isPlus || monthPart.isPlus || dayPart.isPlus || hourPart.isPlus || minutePart.isPlus || weekPart.isPlus) {
                var now = LocalDateTime.now()
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
                else if (minutePart.value > 0)
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
                var now = LocalDateTime.now()
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
                    if (weekDayPart.value == 0)
                        weekDayPart.value = 1
                    if (weekDayPart.value >= weekDayNow) {
                        now = now.plusDays((weekDayPart.value - weekDayNow).toLong())
                    } else {
                        now = now.plusDays((7 - weekDayNow + weekDayPart.value).toLong())
                    }

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
                return null
            }
        }

        class PartResult(
            var isLoop: Boolean = false,
            var isPlus: Boolean = false,
            var isNext: Boolean = false,
            var value: Int = 0,
        )

        private fun parsePart(part: String): PartResult {
            val result = PartResult()
            if (part.startsWith("+")) {
                result.isPlus = true
                result.value = part.substring(1).toInt()
            } else if (part.startsWith("n")) {
                result.isNext = true
                result.value = part.substring(1).toInt()
            } else if (part == "*") {
                result.isLoop = true
            } else {
                result.value = part.toInt()
            }
            return result
        }
    }
}