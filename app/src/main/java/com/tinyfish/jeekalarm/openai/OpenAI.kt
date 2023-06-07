package com.tinyfish.jeekalarm.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleParser
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OpenAI {
    companion object {
        @OptIn(BetaOpenAI::class)
        fun getAnswer(question: String): Schedule? {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val systemContent = """
你现在是一个转换器，输出给程序处理的字符串，请严格输出所需内容，不要添加任何描述文字：
- 当前时间为：${now}
- 把每一句话中与时间有关的部分转换为 crontab 格式并输出，忽略与时间无关的部分。
- 输出中仅包含 crontab 的时间部分，不要包含命令。
- crontab 时间格式包含5个部分，以空格分隔，一定不要出现6个。示例：`21 9 9 5 *` 表示5月9日9点21分。
- 如果是相对时间，例如“两小时以后”，那么结果应该是：`10 23 20 4 *`。
- 没有指定一天中的具体时间，则默认时间是12:00。例如“下个月9号提醒我”转换为`0 12 9 5 *`
- 如果没有指定分钟，则默认为0。
- 如果无法转换，则输出字符串`null`。
"""

            var result: String

            runBlocking {
                val openAI = OpenAI(OpenAIConfig(ConfigService.data.openAiApiKey, LogLevel.All))

                val gpt35turbo = openAI.model(modelId = ModelId("gpt-3.5-turbo"))

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
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                result = openAI.chatCompletion(completionRequest).choices[0].message?.content.orEmpty()
            }

            if (result == "null")
                return null

            val schedule = ScheduleParser.parseStandardCron(result)
            schedule?.onlyOnce = !question.contains("每") && !question.contains("every")
            return schedule
        }
    }
}