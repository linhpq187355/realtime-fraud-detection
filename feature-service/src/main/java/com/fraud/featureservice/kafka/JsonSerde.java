package com.fraud.featureservice.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class JsonSerde<T> implements Serde<T> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Class<T> targetType;

    public JsonSerde(Class<T> targetType) {
        this.targetType = targetType;
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    @Override
    public Serializer<T> serializer() {
        return new Serializer<>() {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public byte[] serialize(String topic, T data) {
                if (data == null) {
                    return null;
                }
                try {
                    return OBJECT_MAPPER.writeValueAsBytes(data);
                } catch (Exception e) {
                    throw new SerializationException("Error serializing JSON message for " + targetType.getSimpleName(), e);
                }
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<>() {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public T deserialize(String topic, byte[] data) {
                if (data == null || data.length == 0) {
                    return null;
                }
                try {
                    return OBJECT_MAPPER.readValue(data, targetType);
                } catch (IOException e) {
                    throw new SerializationException("Error deserializing JSON message for " + targetType.getSimpleName(), e);
                }
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }
}
