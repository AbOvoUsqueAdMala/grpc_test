package org.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.grpc.NumberSequenceGrpc;
import org.example.grpc.NumberSequenceOuterClass;

public class NumberSequenceClient {

    private final ManagedChannel channel;
    private final NumberSequenceGrpc.NumberSequenceStub asyncStub;
    private static final Logger logger = LogManager.getLogger(NumberSequenceClient.class);

    public static void main(String[] args) throws InterruptedException {
        NumberSequenceClient client = new NumberSequenceClient("localhost", 50051);
        try {
            client.getNumbers(0, 30);
            Thread.sleep(60000);
        } finally {
            client.shutdown();
        }
    }

    public NumberSequenceClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = NumberSequenceGrpc.newStub(channel);
    }

    public void getNumbers(int firstValue, int lastValue) {

        NumberSequenceOuterClass.NumberRange request = NumberSequenceOuterClass.NumberRange
                .newBuilder()
                .setFirstValue(firstValue)
                .setLastValue(lastValue)
                .build();

        AtomicInteger responseValue = new AtomicInteger(0);

        StreamObserver<NumberSequenceOuterClass.Number> responseObserver = new StreamObserver<>() {

            @Override
            public void onNext(NumberSequenceOuterClass.Number value) {
                responseValue.set(value.getValue());
                logger.info("New value: {}", responseValue);
            }

            @Override
            public void onError(Throwable t) {
                logger.error(t);
            }

            @Override
            public void onCompleted() {
                logger.info("Stream completed.");
            }
        };

        asyncStub.getNumberSequence(request, responseObserver);

        int currentValue = 0;
        try {
            for (int i = 0; i <= 50; i++) {
                currentValue += responseValue.getAndSet(0) + 1;
                logger.info("Current Value: {}", currentValue);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

}
