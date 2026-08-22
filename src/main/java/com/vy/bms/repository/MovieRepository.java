package com.vy.bms.repository;

import com.vy.bms.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByLanguage(String Language);

    List<Movie> findByGenre(Long id);

    List<Movie> findByTitleContaining(String title);

}
