package com.example.vag.util;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class UploadsInitializer {

    @PostConstruct
    public void init() {
        try {
            String uploadsPath = "D:/Java/apache-tomcat-9.0.97/webapps/vag/uploads/";

            // Создаем основную директорию uploads
            Files.createDirectories(Paths.get(uploadsPath));
            System.out.println("Папка uploads создана: " + uploadsPath);

            // Создаем поддиректории для удобства
            Files.createDirectories(Paths.get(uploadsPath + "artwork-images/"));
            Files.createDirectories(Paths.get(uploadsPath + "user-avatars/"));
            Files.createDirectories(Paths.get(uploadsPath + "temp/"));

            System.out.println("Все поддиректории созданы успешно");

            // Проверяем права доступа
            System.out.println("Проверка прав доступа к папке uploads:");
            System.out.println("Чтение: " + Files.isReadable(Paths.get(uploadsPath)));
            System.out.println("Запись: " + Files.isWritable(Paths.get(uploadsPath)));

        } catch (Exception e) {
            System.err.println("Ошибка при создании папки uploads: " + e.getMessage());
            e.printStackTrace();
        }
    }
}