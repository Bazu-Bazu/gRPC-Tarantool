package com.example.server.dto;

import com.google.protobuf.BytesValue;
import lombok.Builder;

@Builder
public record KVEntryFound(
        boolean exists,
        BytesValue value
) {}
