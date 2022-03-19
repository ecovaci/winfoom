package org.kpax.winfoom.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import lombok.experimental.UtilityClass;
import org.kpax.winfoom.annotation.NotNull;
import org.springframework.util.Assert;

@UtilityClass
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Iterator<String> getFieldNames(@NotNull String json) throws JsonProcessingException {
        Assert.notNull(json, "json cannot be null");
        JsonNode tree = objectMapper.readTree(json);
        return tree.fieldNames();
    }
}
