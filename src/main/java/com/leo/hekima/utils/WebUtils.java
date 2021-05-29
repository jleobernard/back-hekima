package com.leo.hekima.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static com.leo.hekima.utils.JsonUtils.serializeSilentFail;


public class WebUtils {
    private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);
    private static final int MAX_COUNT = 100;
    private static final int DEFAULT_COUNT = 10;

    public record PagingAndSorting(int count, int offset, Sort sort){}

    public static PagingAndSorting getPageAndSort(final ServerRequest request) {
        String _count = request.queryParam("count").orElseGet(()->String.valueOf(DEFAULT_COUNT));
        String _offset = request.queryParam("offset").orElseGet(()->String.valueOf("0"));
        int count, offset;
        try {
            count = Integer.parseInt(_count);
            if(count > MAX_COUNT) {
                count = MAX_COUNT;
            } else if (count < 0) {
                count = DEFAULT_COUNT;
            }
        } catch(Exception e) {
            count = DEFAULT_COUNT;
        }
        try {
            offset = Integer.parseInt(_offset);
            if(offset < 0) {
                offset = 0;
            }
        } catch(Exception e) {
            offset = 0;
        }
        final Sort sort = request.queryParam("sort")
            .map(s -> {
                if(s.startsWith("-")) {
                    return Sort.by(Sort.Direction.DESC, s.substring(1).split(","));
                } else if(s.startsWith("+")) {
                    return Sort.by(Sort.Direction.ASC, s.substring(1).split(","));
                } else {
                    return Sort.by(s.split(","));
                }
            })
            .orElseGet(() -> Sort.by(Sort.DEFAULT_DIRECTION, "id"));
        return new PagingAndSorting(count, offset, sort);
    }


    public static String getOrCreateUri(Object upsertRequest, ServerRequest request) {
        Map<String, String> variables = request.pathVariables();
        if(variables.containsKey("uri")){
            return variables.get("uri");
        }
        final String newUri = StringUtils.sha1InHex(serializeSilentFail(upsertRequest));
        logger.info("Creating uri {} for object of class {}", newUri, upsertRequest.getClass().getCanonicalName());
        return newUri;
    }

    public static ServerResponse.BodyBuilder ok() {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON);
    }
}
