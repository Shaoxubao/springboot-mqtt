package com.baoge.controller;

import com.alibaba.fastjson.JSONObject;
import com.baoge.gateway.GatewayService;
import com.baoge.utils.ApplicationContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.baoge.config.MqttProviderConfig;

@Controller
public class SendController {
    @Autowired
    private MqttProviderConfig providerClient;

    @RequestMapping("/sendMessage")
    @ResponseBody
    public String sendMessage(int qos, boolean retained, String topic, String message) {
        try {
            providerClient.publish(qos, retained, topic, message);
            return "发送成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "发送失败";
        }
    }

    @RequestMapping("/sendMessage2")
    @ResponseBody
    public String sendMessage2(String message) {
        try {
            GatewayService gatewayService = ApplicationContextUtil.getBean(GatewayService.class);
            JSONObject body = new JSONObject();
            body.put("device", message);
            gatewayService.onDeviceTelemetry(body);
            return "发送成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "发送失败";
        }
    }
}