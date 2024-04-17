package org.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.example.grpc.NumberSequenceGrpc;
import org.example.grpc.NumberSequenceOuterClass;

public class NumberSequenceClient {

    private final ManagedChannel channel;
    private final NumberSequenceGrpc.NumberSequenceStub asyncStub;

    public NumberSequenceClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = NumberSequenceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void getNumbers(int firstValue, int lastValue) {

        NumberSequenceOuterClass.NumberRange request = NumberSequenceOuterClass.NumberRange
                .newBuilder()
                .setFirstValue(firstValue)
                .setLastValue(lastValue)
                .build();

        final BlockingQueue<NumberSequenceOuterClass.Number> responses = new ArrayBlockingQueue<>(1);

        StreamObserver<NumberSequenceOuterClass.Number> responseObserver = new StreamObserver<>() {

            @Override
            public void onNext(NumberSequenceOuterClass.Number value) {
                responses.offer(value);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed.");
            }
        };

        asyncStub.getNumberSequence(request, responseObserver);

        new Thread(() -> {
            int currentValue = 0;
            try {
                for (int i = 0; i <= 50; i++) {
                    Thread.sleep(1000);
                    NumberSequenceOuterClass.Number number = responses.poll();
                    if (number != null) {
                        System.out.println("New value: " + number.getValue());
                    }
                    int lastNumber = number != null ? number.getValue() : 0;
                    currentValue += lastNumber + 1;
                    System.out.println("Current value: " + currentValue);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static void main(String[] args) throws InterruptedException {
        NumberSequenceClient client = new NumberSequenceClient("localhost", 50051);
        try {
            client.getNumbers(0, 30);
            Thread.sleep(60000);
        } finally {
            client.shutdown();
        }
    }
}
