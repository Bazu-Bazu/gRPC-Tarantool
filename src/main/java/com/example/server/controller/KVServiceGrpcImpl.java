package com.example.server.controller;

import com.example.server.repository.KVRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import kv.KVServiceOuterClass;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class KVServiceGrpcImpl extends kv.KVServiceGrpc.KVServiceImplBase {

    private final KVRepository repository;

    @Override
    public void put(
            KVServiceOuterClass.PutRequest request,
            StreamObserver<KVServiceOuterClass.PutResponse> responseObserver
    ) {
        try {
            byte[] valueBytes = request.hasValue() ? request.getValue().toByteArray() : null;
            repository.put(request.getKey(), valueBytes);

            var response = KVServiceOuterClass.PutResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to put key: " + request.getKey())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void get(
            KVServiceOuterClass.GetRequest request,
            StreamObserver<KVServiceOuterClass.GetResponse> responseObserver
    ) {
        try {
            var valueDto = repository.get(request.getKey());

            var responseBuilder = KVServiceOuterClass.GetResponse.newBuilder();

            if (valueDto.exists()) {
                responseBuilder.setFound(true);

                if (valueDto.value() != null) {
                    responseBuilder.setValue(valueDto.value());
                }
            } else {
                responseBuilder.setFound(false);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get key: " + request.getKey())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void delete(
            KVServiceOuterClass.DeleteRequest request,
            StreamObserver<KVServiceOuterClass.DeleteResponse> responseObserver
    ) {
        try {
            repository.delete(request.getKey());

            var response = KVServiceOuterClass.DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to delete key: " + request.getKey())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void range(
            KVServiceOuterClass.RangeRequest request,
            StreamObserver<KVServiceOuterClass.RangeResponse> responseObserver
    ) {
        try {
            repository.streamRange(request.getKeySince(), request.getKeyTo(), (key, value) -> {
                var responseBuilder = KVServiceOuterClass.RangeResponse.newBuilder()
                        .setKey(key);

                if (value != null) {
                    responseBuilder.setValue(value.getValue());
                }

                responseObserver.onNext(responseBuilder.build());
            });

            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    String.format("Range for key_since %s to key_to %s failed",
                                            request.getKeySince(), request.getKeyTo())
                            )
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void count(
            KVServiceOuterClass.CountRequest request,
            StreamObserver<KVServiceOuterClass.CountResponse> responseObserver
    ) {
        try {
            long count = repository.count();

            var response = KVServiceOuterClass.CountResponse.newBuilder()
                    .setCount(count)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get count keys")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
