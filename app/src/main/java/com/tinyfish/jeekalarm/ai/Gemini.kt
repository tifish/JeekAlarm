package com.tinyfish.jeekalarm.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Gemini {
    companion object {
        suspend fun getAnswer(question: String): Schedule? {
            val config = generationConfig {
                temperature = 0f
                topK = 1
                topP = 0f
                maxOutputTokens = 32
            }
            val generativeModel = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = SettingsService.geminiKey,
                generationConfig = config
            )

            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val askContent =
                if (question.contains("每") || question.contains("every ")) question
                else "$question（当前时间：$now）"
            val inputContent = """
你现在是一个转换器，输出给程序处理的字符串：
- 把每一句话中与时间有关的部分转换为 crontab 格式并输出，忽略与时间无关的部分。
- crontab 格式最后新增一个字段，表示年份，所以输入字段一共是6个，对应分、时、日、月、周、年，以空格分隔。
- 如果描述的是相对时间，则结尾会有当前时间，请根据当前时间计算。
- 相对时间的月份循环往复，例如12月的下个月是1月。
- 周日对应数字7。
- 如果没有指定是那一周，那么不要填写日期、月份、年份，只填写星期几。
- 如果没有指定分钟，则默认为0分。
- 如果没有指定一天中的几点，则默认时间是12点。
- 如果没有指定一月中的哪一天，则默认时间是1号。
- 如果没有指定一年中的哪一月，则默认时间是1月。
- 如果无法转换，则输出字符串`null`。

input: 5分钟后提醒我（当前时间：2023-04-20 21:10）
output: 15 21 20 4 * 2023
input：周五下午3点开会（当前时间：2023-04-20 21:10）
output: 0 15 * * 5 2023
input：下周五下午3点开会（当前时间：2023-04-20 21:10）
output: 0 15 28 4 5 2023
input：周六晚上8点去看电影（当前时间：2023-04-20 21:10）
output: 0 20 * * 6 *
input：下个月14号去银行办理社保卡（当前时间：2023-04-20 21:10）
output: 0 12 14 5 * 2023
input：2月去种牙（当前时间：2023-04-20 21:10）
output: 0 12 1 2 * 2024
input：7月提醒我去旅游（当前时间：2023-04-20 21:10）
output: 0 12 1 7 * 2023
input: 每年5月9日晚上9点提醒我
output: 0 21 9 5 * *
input: 每周日中午12点30转账
output: 30 12 * * 7 *
input: 每周二晚上7点打电话给妈妈
output: 0 19 * * 2 *
input: 每个月6号还信用卡
output: 0 12 6 * * *
input: 每天早上7点起床
output: 0 7 * * * *

input: $askContent
output: 
"""

            val response = generativeModel.generateContent(inputContent)
            if (response.text == null)
                return null
            val result = response.text!!
            if (result == "null")
                return null

            val schedule = ScheduleParser.parseStandardCron(result)
            schedule?.onlyOnce = !question.contains("每") && !question.contains("every ")

            return schedule
        }
    }
}