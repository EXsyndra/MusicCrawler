package com.yrw.crawler.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yrw.crawler.impl.MultiCrawlerWithMybatis;
import com.yrw.crawler.mapper.SongMapper;

@Controller
@RequestMapping("/crawler")
public class CrawlerController {
    
    @Autowired
    private MultiCrawlerWithMybatis multiCrawler;
    
    @Autowired
    private SongMapper songMapper;
    
    @ResponseBody
    @GetMapping("/start")
    public String start() throws InterruptedException {
        multiCrawler.run();
        return "爬取完毕";
    }
    
    @GetMapping("/songs")
    public String songs(Model model, @RequestParam("page") Optional<Integer> page) {
    		PageHelper.startPage(page.orElse(1), 20);
        model.addAttribute("songs", new PageInfo<>(songMapper.findAll()));
        return "songs";
    }
    
}