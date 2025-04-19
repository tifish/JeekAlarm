package com.tinyfish.jeekalarm.ifly

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechUtility
import com.iflytek.cloud.ui.RecognizerDialog
import com.iflytek.cloud.ui.RecognizerDialogListener
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.start.App

// Reference: https://www.xfyun.cn/doc/asr/voicedictation/Android-SDK.html#_2%E3%80%81sdk%E9%9B%86%E6%88%90%E6%8C%87%E5%8D%97
class IFly {
    companion object {
        private fun initOnce(): Boolean {
            if (SettingsService.iFlyAppId == "")
                return false

            if (SpeechUtility.getUtility() != null)
                return true

            SpeechUtility.createUtility(App.context, SpeechConstant.APPID + "=" + SettingsService.iFlyAppId)

            return SpeechUtility.getUtility() != null
        }

        fun showDialog(context: Context, onResult: (String) -> Unit) {
            if (!initOnce())
                return

            val initListener = InitListener { code ->
                if (code != ErrorCode.SUCCESS) {
                    Toast.makeText(context, "IFly init error code：$code", Toast.LENGTH_SHORT).show()
                }
            }

            val recognizerDialog = RecognizerDialog(context, initListener)
            recognizerDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn") // zh_cn, en_us
            recognizerDialog.setParameter(SpeechConstant.ACCENT, "mandarin") // 普通话
            recognizerDialog.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8")
            // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
            recognizerDialog.setParameter(SpeechConstant.ASR_PTT, "0")
            // 设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
            recognizerDialog.setParameter(SpeechConstant.RESULT_TYPE, "plain")
            // 设置语音前端点:静音超时时间，单位ms，即用户多长时间不说话则当做超时处理
            // 取值范围{1000～10000}
            recognizerDialog.setParameter(SpeechConstant.VAD_BOS, "2000")
            // 设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
            // 自动停止录音，范围{0~10000}
            recognizerDialog.setParameter(SpeechConstant.VAD_EOS, "2000")

            var resultString = ""

            recognizerDialog.setListener(object : RecognizerDialogListener {
                // 返回结果，第二个参数为 false 表示还有更多结果，为 true 表示最后一个结果
                override fun onResult(recognizerResult: com.iflytek.cloud.RecognizerResult?, b: Boolean) {
                    if (recognizerResult != null) {
                        resultString += recognizerResult.resultString
                    }

                    if (b) {
                        onResult(resultString)
                    }
                }

                override fun onError(speechError: SpeechError) {
                    Log.e("IFly recognizing failed", speechError.errorCode.toString() + "")
                }
            })

            // 显示讯飞语音识别视图
            recognizerDialog.show()
        }
    }
}