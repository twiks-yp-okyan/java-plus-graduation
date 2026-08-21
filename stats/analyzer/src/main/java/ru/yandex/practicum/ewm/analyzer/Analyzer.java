package ru.yandex.practicum.ewm.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.ewm.analyzer.service.processor.EventSimilarityEventProcessor;
import ru.yandex.practicum.ewm.analyzer.service.processor.UserActionEventProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(Analyzer.class, args);

        final EventSimilarityEventProcessor eventSimilarityEventProcessor = context.getBean(EventSimilarityEventProcessor.class);
        UserActionEventProcessor userActionEventProcessor = context.getBean(UserActionEventProcessor.class);

        // запускаем в отдельном потоке обработчик событий для схожести событий
        Thread eventSimilarityThread = new Thread(eventSimilarityEventProcessor);
        eventSimilarityThread.setName("EventSimilarityHandlerThread");
        eventSimilarityThread.start();

        // В текущем потоке начинаем обработку действий пользователей
        userActionEventProcessor.start();
    }
}
