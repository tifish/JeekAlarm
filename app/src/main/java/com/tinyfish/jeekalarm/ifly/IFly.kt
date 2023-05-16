package com.tinyfish.jeekalarm.ifly

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.Setting
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechUtility
import com.iflytek.cloud.ui.RecognizerDialog
import com.iflytek.cloud.ui.RecognizerDialogListener
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.start.App


class IFly {
    companion object {
        private fun initOnce(): Boolean {
            if (ConfigService.data.iFlyAppId == "")
                return false

            if (SpeechUtility.getUtility() != null)
                return true

            SpeechUtility.createUtility(App.context, SpeechConstant.APPID + "=" + ConfigService.data.iFlyAppId)
            Setting.setLocationEnable(false)

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

            recognizerDialog.setListener(object : RecognizerDialogListener {
                override fun onResult(recognizerResult: com.iflytek.cloud.RecognizerResult?, b: Boolean) { //返回结果
                    if (recognizerResult == null) {
                        onResult("")
                        return
                    }

                    var result = parseResult(recognizerResult.resultString)
                    result = result.trimEnd('。')

                    onResult(result)
                }

                override fun onError(speechError: SpeechError) {
                    Log.e("IFly recognizing failed", speechError.errorCode.toString() + "")
                }
            })

            // 显示讯飞语音识别视图
            recognizerDialog.show()
        }

        private fun parseResult(resultString: String): String {
            val jsonObject: JSONObject = JSON.parseObject(resultString)
            val jsonArray: JSONArray = jsonObject.getJSONArray("ws")
            val stringBuffer = StringBuffer()
            for (i in 0 until jsonArray.size) {
                val jsonObject1: JSONObject = jsonArray.getJSONObject(i)
                val jsonArray1: JSONArray = jsonObject1.getJSONArray("cw")
                val jsonObject2: JSONObject = jsonArray1.getJSONObject(0)
                val w: String = jsonObject2.getString("w")
                stringBuffer.append(w)
            }

            return stringBuffer.toString()
        }
    }
}