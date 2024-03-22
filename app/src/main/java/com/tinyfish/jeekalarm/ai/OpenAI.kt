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
你现在是一个转换器，输出给程序处理的字符串：
- 输入是一个闹钟时间和相关的事件。
- 如果闹钟是一个绝对时间，请输出时间，格式为: yyyy-mm-dd hh:MM
- 如果闹钟是一个相对时间，请输出相对时间，格式为: +yyyy-mm-dd hh:MM ww-aa
    - ww表示周数，例如下周就是01，下下周就是02。如果没有周数描述，ww为00。
    - aa表示星期几，星期一为01，星期日为07。如果没有星期几的描述，aa为00。
- 如果闹钟包含每天、每周、每月、每年等描述，表示是循环闹钟，请输出循环时间，格式为: @yyyy-mm-dd hh:MM aa
    - aa表示星期几，星期一为01，星期日为07。如果没有星期几的描述，aa为00。
- 本周几这样的描述当做相对时间来处理。
- 输出时间使用24小时制。
- 没有描述的日期部分请输出00，时间部分请输出-1。
- 如果没有时间描述，请输出null
- 以下是一些例子：
    - 下午两点半: 0000-00-00 14:30
    - 12月吃火锅: 0000-12-00 -1:-1
    - 后天下午3点: +0000-00-02 15:00 00-00
    - 下周五晚上8点: +0000-00-00 20:00 01-05
    - 下下个月20号: +0000-02-20 -1:-1 00-00
"""

            val openAI = OpenAI(OpenAIConfig(SettingsService.openAiApiKey, LoggingConfig(LogLevel.All)))
            val gpt35turbo = openAI.model(modelId = ModelId("gpt-4-turbo-preview"))
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
            return if (gptResult == "null") {
                null
            } else if (gptResult.startsWith('@')) {
                parseCycleAlarm(gptResult)
            } else if (gptResult.startsWith('+')) {
                parseRelativeAlarm(gptResult)
            } else {
                parseAbsoluteAlarm(gptResult)
            }
        }

        private fun parseCycleAlarm(gptResult: String): Schedule? {
            // @yyyy-mm-dd hh:MM aa
            val parts = gptResult.trimStart('@').split('-', ':', ' ')
            if (parts.size != 6)
                return null
            var year = parts[0].toInt()
            var month = parts[1].toInt()
            var day = parts[2].toInt()
            var hour = parts[3].toInt()
            var minute = parts[4].toInt()
            var weekDay = parts[5].toInt()

            year = 0
            if (month > 0) {
                day = maxOf(day, 1)
            }

            if (hour == -1 && minute == -1) {
                hour = 12
                minute = 0
            }

            return Schedule(
                yearConfig = if (year == 0) "*" else year.toString(),
                monthConfig = if (month == 0) "*" else month.toString(),
                dayConfig = if (day == 0) "*" else day.toString(),
                hourConfig = if (hour == 0) "*" else hour.toString(),
                minuteConfig = if (minute == 0) "*" else minute.toString(),
                weekDayConfig = if (weekDay == 0) "*" else weekDay.toString(),
            ).apply {
                onlyOnce = false
            }
        }

        private fun parseRelativeAlarm(gptResult: String): Schedule? {
            // +yyyy-mm-dd hh:MM ww-aa
            val parts = gptResult.trimStart('+').split('-', ':', ' ')
            if (parts.size != 7)
                return null
            var year = parts[0].toInt()
            var month = parts[1].toInt()
            var day = parts[2].toInt()
            var hour = parts[3].toInt()
            var minute = parts[4].toInt()
            var week = parts[5].toInt()
            var weekDay = parts[6].toInt()

            var now = LocalDateTime.now()

            if (year > 0) {
                now = now.plusYears(year.toLong())
                year = now.year
                weekDay = 0
            } else if (month > 0) {
                now = now.plusMonths(month.toLong())
                year = now.year
                month = now.monthValue
                weekDay = 0
            } else if (day > 0) {
                now = now.plusDays(day.toLong())
                year = now.year
                month = now.monthValue
                day = now.dayOfMonth
                weekDay = 0
            } else if (week > 0) {
                val weekDayNow = now.dayOfWeek.value
                if (weekDay == 0)
                    weekDay = 1
                now = now.plusDays((week * 7 - weekDayNow + weekDay).toLong())
                year = now.year
                month = now.monthValue
                day = now.dayOfMonth
            } else if (weekDay > 0) {
                val weekDayNow = now.dayOfWeek.value
                if (weekDay > weekDayNow) {
                    now = now.plusDays((weekDay - weekDayNow).toLong())
                } else {
                    now = now.plusDays((7 - weekDayNow + weekDay).toLong())
                }
                year = now.year
                month = now.monthValue
                day = now.dayOfMonth
            } else if (hour > 0) {
                now = now.plusHours(hour.toLong())
                year = now.year
                month = now.monthValue
                day = now.dayOfMonth
                hour = now.hour
            } else if (minute > 0) {
                now = now.plusMinutes(minute.toLong())
                year = now.year
                month = now.monthValue
                day = now.dayOfMonth
                hour = now.hour
                minute = now.minute
            } else {
                return null
            }

            if (month == 0)
                month = 1
            if (day == 0)
                day = 1
            if (hour == -1 && minute == -1) {
                hour = 12
                minute = 0
            }

            return Schedule(
                yearConfig = year.toString(),
                monthConfig = month.toString(),
                dayConfig = day.toString(),
                hourConfig = hour.toString(),
                minuteConfig = minute.toString(),
                weekDayConfig = if (weekDay == 0) "*" else weekDay.toString(),
            ).apply {
                onlyOnce = true
            }
        }

        private fun parseAbsoluteAlarm(gptResult: String): Schedule? {
            // yyyy-mm-dd hh:MM
            val parts = gptResult.split('-', ':', ' ')
            if (parts.size != 5)
                return null
            var year = parts[0].toInt()
            var month = parts[1].toInt()
            var day = parts[2].toInt()
            var hour = parts[3].toInt()
            var minute = parts[4].toInt()

            val now = LocalDateTime.now()
            if (year == 0) {
                year = now.year
                if (month == 0) {
                    month = now.monthValue
                    if (day == 0) {
                        day = now.dayOfMonth
                    }
                }
            }

            if (month == 0)
                month = 1
            if (day == 0)
                day = 1
            if (hour == -1 && minute == -1) {
                hour = 12
                minute = 0
            }

            return Schedule(
                yearConfig = year.toString(),
                monthConfig = month.toString(),
                dayConfig = day.toString(),
                hourConfig = hour.toString(),
                minuteConfig = minute.toString(),
                weekDayConfig = "*",
            ).apply {
                onlyOnce = true
            }
        }
    }
}