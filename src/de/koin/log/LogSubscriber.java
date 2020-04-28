package de.koin.log;

import de.koin.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class LogSubscriber implements Flow.Subscriber<String> {

    private LogTail logTail;
    private Flow.Subscription subscription;

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

    public LogSubscriber() {
        try {
            logTail = new LogTail(new File(Config.getLogPath()), publisher);
            publisher.subscribe(this);
            executorService.execute(logTail);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String item) {
        System.out.println(item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        try {
            logTail.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
