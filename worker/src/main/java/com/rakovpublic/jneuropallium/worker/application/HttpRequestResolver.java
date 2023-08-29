package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.net.URI;
import java.net.http.HttpRequest;

public class HttpRequestResolver {

    public static <K extends Object> HttpRequest createPost(String uri, K payload) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(payload);
        return HttpRequest.newBuilder(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(json)).build();
    }
}
