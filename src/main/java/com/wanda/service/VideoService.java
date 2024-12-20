package com.wanda.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoService {

    @Value("${video.directory}")
    private String VideoDIR;

    public FileSystemResource getMasterFile(String videoId, String resolution) {

        Path filePath = Paths.get(VideoDIR, videoId, resolution, "master.m3u8");

        System.out.println("filePath: " + filePath);

        return new FileSystemResource(filePath.toFile());
    }

}
