package com.hero.basiclib.http;

import android.util.Log;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;

public class JsonSerializer {
    private static final String TAG = JsonSerializer.class.getName();

    private static JsonSerializer instance = new JsonSerializer();

    private ObjectMapper impl;

    private JsonSerializer() {
        impl = new ObjectMapper();
        impl.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
    }

    public static JsonSerializer getInstance() {
        if (instance == null) {
            instance = new JsonSerializer();
        }
        return instance;
    }

    public ObjectMapper getMappter() {
        return impl;
    }

    public String serialize(Object object) {
        try {
            return impl.writeValueAsString(object);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "err:", e);
            return null;
        }
    }

    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return impl.readValue(json, clazz);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public <T> T deserialize(Reader reader, Class<T> clazz) {
        try {
            return impl.readValue(reader, clazz);
        } catch (Exception e) {

            return null;
        }
    }

    public <T> T deserialize(String json, TypeReference<T> tf) {
        try {
            return impl.readValue(json, tf);
        } catch (Exception e) {

            return null;
        }
    }


    /**
     * 不吞掉异常
     *
     * @param reader
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T deserializeFrankly(Reader reader, Class<T> clazz) throws IOException, JsonParseException, JsonMappingException {
        return impl.readValue(reader, clazz);
    }

    public <T extends Collection<?>, V> Object deserialize(String json,
                                                           Class<T> collection, Class<V> data) {
        try {
            return impl.readValue(json,
                    impl.getTypeFactory().constructCollectionType(collection, data));
        } catch (Exception e) {

            return null;
        }
    }
}
