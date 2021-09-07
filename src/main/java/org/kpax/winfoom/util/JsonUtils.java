package org.kpax.winfoom.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.kpax.winfoom.annotation.NotNull;
import org.springframework.util.Assert;

import java.util.Iterator;

@UtilityClass
public class JsonUtils {

    public static Iterator<String> getFieldNames(@NotNull String json) throws JsonProcessingException {
        Assert.notNull(json, "json cannot be null");
        JsonNode tree = new ObjectMapper().readTree(json);
        return tree.fieldNames();
    }
}
