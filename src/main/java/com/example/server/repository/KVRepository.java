package com.example.server.repository;

import com.example.server.dto.KVEntryFound;
import com.google.protobuf.BytesValue;

public interface KVRepository {

    void put(String key, byte[] value);

    KVEntryFound get(String key);

    void delete(String key);

    long count();

    void streamRange(String keySince, String keyTo, RangeConsumer consumer);

    interface RangeConsumer {
        void accept(String key, BytesValue value);
    }
}
