/**
 * Copyright © 2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baoge.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by ashvayka on 19.01.17.
 */
public class JsonTools {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static ObjectNode newNode() {
        return JSON.createObjectNode();
    }

    public static byte[] toBytes(ObjectNode node) {
        return toString(node).getBytes(StandardCharsets.UTF_8);
    }

    public static JsonNode fromString(String data) {
        try {
            return JSON.readTree(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromString(String data, TypeReference<T> type) {
        try {
            return JSON.readValue(data, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(JsonNode node) {
        try {
            return JSON.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(Map<String, String> map) {
        try {
            return JSON.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
