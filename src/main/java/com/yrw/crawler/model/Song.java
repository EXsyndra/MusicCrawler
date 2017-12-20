package com.yrw.crawler.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

public class Song implements Serializable{
    
	private Long id;
    private String url;
    private String title;
    private Long commentCount;
    
    public Song() {
    		super();
    }
    
    public Song(String url, String title) {
        this();
        this.id = Long.parseLong(url.substring(30));
        this.url = url;
        this.title = title;
    }
    
    public Song(String url, String title, Long commentCount) {
    		this(url, title);
        this.commentCount = commentCount;
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    @Override
    public String toString() {
        return "Song [url=" + url + ", title=" + title + ", commentCount=" + commentCount + "]";
    }

}
