package org.kpax.winfoom.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kpax.winfoom.annotation.NotNull;
import org.springframework.util.Assert;

import java.util.Iterator;

public class JsonUtils {
    private JsonUtils() {
    }

    public static Iterator<String> getFieldNames(@NotNull String json) throws JsonProcessingException {
        Assert.notNull(json, "json cannot be null");
        JsonNode tree = new ObjectMapper().readTree(json);
        return tree.fieldNames();
    }
}
