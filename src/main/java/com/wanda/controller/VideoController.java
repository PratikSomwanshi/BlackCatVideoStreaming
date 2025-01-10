package com.wanda.controller;


import com.wanda.dto.FileDTO;
import com.wanda.entity.Video;
import com.wanda.service.VideoService;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import com.wanda.utils.exceptions.enums.SuccessCode;
import com.wanda.utils.exceptions.response.SuccessResponse;
import org.apache.coyote.Response;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1")
public class VideoController {

    private final String CLOUDFLARE_URL = "https://pub-454ff1c8ffec4fc1981ce8c81e28d2f3.r2.dev";

    private VideoService videoService;





    public VideoController(VideoService videoService) {
        this.videoService = videoService;

    }

    @GetMapping("/video/search")
    public ResponseEntity<?> searchVideo(@Param("query") String query) {
        var videos = this.videoService.searchVideos(query);

        var success = new SuccessResponse<>(
                "Successfully fetched all videos",
                SuccessCode.GENERAL_SUCCESS,
                videos
        );

        return new ResponseEntity<>(success, HttpStatus.OK);
    }

    @PostMapping("/single/video")
    public ResponseEntity<Object> createVideo(@RequestBody Video video) {


        var existingVideo = this.videoService.findOneVideo(video.getId());

        var success = new SuccessResponse<>(
                "Successfully fetched the video",
                SuccessCode.GENERAL_SUCCESS,
                existingVideo
        );

        return new ResponseEntity<>(success, HttpStatus.OK);
    }

    @GetMapping("/video")
    public ResponseEntity<?> findAllVideo(){
        var res = this.videoService.findAllVideo();

        var success = new SuccessResponse<>(
                "Successfully fetched all videos",
                SuccessCode.GENERAL_SUCCESS,
                res
        );

        return new ResponseEntity<>(success, HttpStatus.OK);
    }


    @GetMapping("/video/hls/{videoId}/{resolution}/playlist.m3u8")
    public ResponseEntity<FileDTO> playlistVideo(@PathVariable String videoId, @PathVariable String resolution) {
        System.out.println("Hitting vide playlist");

        String fileUrl = CLOUDFLARE_URL + "/" + "master.m3u8";

        var file = new FileDTO();
        file.setFileURL(fileUrl);

        return ResponseEntity
                .ok()
                .body(file);
    }



}


