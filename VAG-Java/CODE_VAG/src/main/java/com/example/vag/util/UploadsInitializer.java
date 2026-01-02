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
            String uploadsPath = "D:/Java/apache-tomcat-9.0.104/webapps/vag/uploads/";
//             String uploadsPath = "D:/Java/apache-tomcat-9.0.97/webapps/vag/uploads/";
            Files.createDirectories(Paths.get(uploadsPath));
            System.out.println("Папка uploads создана: " + uploadsPath);
        } catch (Exception e) {
            System.err.println("Ошибка при создании папки uploads: " + e.getMessage());
        }
    }
}