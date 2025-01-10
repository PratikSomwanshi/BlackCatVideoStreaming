package com.wanda.repository;

import com.wanda.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {

    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(v.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Video> searchVideos(String query);
}
