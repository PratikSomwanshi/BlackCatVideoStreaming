package com.wanda.service;

import com.wanda.entity.Video;
import com.wanda.repository.UsersRepository;
import com.wanda.repository.VideoRepository;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class VideoService {



    private VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public List<Video> searchVideos(String query) {
        return videoRepository.searchVideos(query);
    }

//    public FileSystemResource getMasterFile(String videoId, String resolution) {

//        System.out.println("Video Dir: " + VideoDIR);
//        Path filePath = Paths.get(VideoDIR, videoId, resolution, "master.m3u8");

//        System.out.println("filePath: " + filePath);
//
//        return new FileSystemResource(filePath.toFile());
//    }

    public List<Video> findAllVideo() {
        return this.videoRepository.findAll();
    }

    public Video findOneVideo(String id) {

        var video = this.videoRepository.findById(id);

        if (video.isEmpty()){
            throw new CustomException("Video not found", HttpStatus.NOT_FOUND ,ErrorCode.FILE_NOT_FOUND);
        }

        return video.get();
    }
}
