package com.back.domain.news.fake.repository;

import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.real.entity.RealNews;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FakeNewsRepository extends JpaRepository<FakeNews, Long> {

}
