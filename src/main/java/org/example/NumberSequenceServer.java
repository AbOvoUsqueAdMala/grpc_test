package org.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.example.grpc.NumberSequenceGrpc;
import org.example.grpc.NumberSequenceOuterClass;

import java.io.IOException;

public class NumberSequenceServer {

    private Server server;
    private static final int PORT = 50051;

    public static void main(String[] args) throws IOException, InterruptedException {
        final NumberSequenceServer server = new NumberSequenceServer();
        server.start();
        server.blockUntilShutdown();
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(PORT)
                .addService(new NumberSequenceImpl())
                .build()
                .start();
        System.out.println("Server started, listening on " + PORT);
    }

    static class NumberSequenceImpl extends NumberSequenceGrpc.NumberSequenceImplBase {

        @Override
        public void getNumberSequence(NumberSequenceOuterClass.NumberRange request,
                                      StreamObserver<NumberSequenceOuterClass.Number> responseObserver) {
            try {
                int value = request.getFirstValue();
                while (value <= request.getLastValue()) {
                    NumberSequenceOuterClass.Number number = NumberSequenceOuterClass.Number
                            .newBuilder()
                            .setValue(value)
                            .build();
                    responseObserver.onNext(number);
                    value++;
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            responseObserver.onCompleted();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

}

