package com.wanda.controller;


import com.wanda.service.VideoService;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1")
public class VideoController {

    private VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }


    @GetMapping("/video/hls/{videoId}/{resolution}/playlist.m3u8")
    public ResponseEntity<FileSystemResource> playlistVideo(@PathVariable String videoId, @PathVariable String resolution) {
        FileSystemResource masterFile = this.videoService.getMasterFile(videoId, resolution);

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(masterFile);
    }

    @GetMapping("/video/hls/{videoId}/{resolution}/{segmentName}")
    public ResponseEntity<Resource> serveHlsSegment(@PathVariable String videoId, @PathVariable String segmentName, @PathVariable String resolution) {

        String filePath = "videos/" + videoId + "/" + resolution + "/" + segmentName;

        System.out.println(filePath);

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new CustomException("File not found", HttpStatus.NOT_FOUND, ErrorCode.FILE_NOT_FOUND);
        }


        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/MP2T"))
                .body(resource);

    }

}


