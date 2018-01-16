# 网易云音乐爬虫
一个简单的爬虫，可以得到网易云音乐的评论数，并由评论从高到低展示出来。
一个最简单的爬虫的流程：
![]()
## 目录
```
├── com.yrw.crawler
├── 	Application.java			//启动Spring boot
├── 	Crawler.java				// 爬虫的接口，定义了爬虫重要方法
├── 	HtmlFetcher.java			//获取Html页面的方法
├── 	HtmlParser.javar			//解析Html页面的方法
├── 	com.yrw.crawler.impl
├── 		MultiCrawlerThread.java			//爬虫的流程
├── 		MultiCrawlerWithMybatis.java		//爬虫的实现类，用mybatis实现
├── 	com.yrw.crawler.controller			//controller层，用于展示爬虫的结果
├── 	com.yrw.crawler.mapper			// mapper层，用mybatis实现数据持久化
├── 	com.yrw.model			// 数据模型
├── 	com.yrw.task			//定时任务
```
#### 建立4个表：
```
create table song(
    id int(11) primary key, 
    url varchar(100) not null, 
    title varchar(21688) not null, 
    comment_count int(11) not null
);
```
**song表用来存放歌曲信息。**
```
create table playlists_page(
    id varchar(35) primary key, 
    url varchar(100) not null,  
    title varchar(21688) not null, 
    status char(10) not null, 
    type char(10) not null
);

create table playlist_page(
    ......
);

create table song_page(
    ......
);
```
**这三个表存放页面信息，分为存放：**

[歌单列表页面](http://music.163.com/#/discover/playlist)

[歌单页面](http://music.163.com/#/discover/playlist)

[歌曲页面](http://music.163.com/#/song?id=143238)


### 详细流程
- #### 初始化
初始化所有的分类信息，在运行爬虫。
```
public interface Crawler {

    default void run() {
        init("全部");            
        init("华语");
        init("欧美");
        ......

        init("儿童");
        init("榜单");
        doRun();
    }
}
```
```
@Override
public class MultiCrawlerWithMybatis implements Crawler {

    //init方法的实现
    public void init(String catalog) {
        for(int i = 0; i < 43; i++) {
            WebPage webPage = new WebPage(
                "http://music.163.com/discover/playlist/"
                + "?order=hot&cat="+catalog+"&limit=35&offset="  + (i * 35), 
                PageType.playlists);
            webPage.setId(catalog+i);
            webPageMapper.save(webPage);
        }
    }
}
```
- #### 获取页面
顺序是：歌单列表页面->歌单页面->歌曲页面，获取到页面后把状态改成crawled。
```
@Override
public synchronized WebPage getUnCrawlPage() {
    WebPage webPage = webPageMapper.findUncrawledTop(PageType.playlists);
    if(webPage == null) webPage = webPageMapper.findUncrawledTop(PageType.playlist);
    webPage = webPageMapper.findUncrawledTop(PageType.song);
    if(webPage == null) {
        return null;
    }
    setToCrawled(webPage);
    return webPage;
}
```
- #### 将页面保存到爬虫队列里
```
@Override
public List<WebPage> addToCrawlList(List<WebPage> webPages) {
    for(WebPage webPage:webPages) {
        //歌曲名字过长的忽略掉
        if(webPage.getTitle().length()>21688) {
            webPageMapper.updateStatus(webPage, Status.crawled);
            continue;
        }
        //保存
        saveWebPage(webPage);
    }
    return webPages;
}
public synchronized void saveWebPage(WebPage webPage) {
    //检查这个页面是不是已经在队列里了
    if(webPageMapper.findWebPage(webPage) != null) {return;}
    webPageMapper.save(webPage);
}
```
- #### 解析页面
我用jsoup来获取页面信息并解析，使用方法参见：

- [官方文档](https://jsoup.org/)

- [中文版手册](http://www.open-open.com/jsoup/)

- #### 关于如何得到评论数：
打开谷歌浏览器开发者工具，在xhr请求中寻找得到评论数的请求，可以找到
![]()
观察这个请求，发现是一个post请求，参数是：
![]()
![]()
只要得到这两个参数，再post请求发送过去就可以得到评论数了。
所以问题转换为：如何得到这两个参数。

查看一下发起请求的是core.js，
![]()
把这个地址放到迅雷里面，下载这个js文件，美化一下。
在文件中搜索 "encSecKey"，可以找到：
![]()
```
var bAN7G = window.asrsea(JSON.stringify(j5o), bnQ3x(["流泪", "强"]), bnQ3x(Mc1x.md), bnQ3x(["爱心", "女孩", "惊恐", "大笑"]));
e5j.data = k5p.dh7a({
    params: bAN7G.encText,
    encSecKey: bAN7G.encSecKey
})
```
bAN7G变量是通过window.asrsea()函数得到的，共有四个参数，我称之为**参数1，参数2，参数3，参数4**。于是搜索这个函数，可以找到：
![]()
**encText**即post的第一个参数param，**encSecKey**即第二个参数encSecKey。

通过查看上面的函数可以发现：
a函数用于生成一个16位随机数；b是一个aes加密函数，加密模式是CBC；
c是rsa加密函数。
```
result=b(参数1， 参数4);  
encText = b(result，16位随机数);
encSecKey = c(16位随机数,  参数2,  参数3);
```
以上，加密的过程就知道了。
接下来，问题转换为得到这四个参数的值。
```
参数1：一个json
text = "{\"username\": \"\", \"rememberLogin\": \"true\", \"password\": \"\"}";
参数2："010001"
参数3："00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"
参数4："0CoJUm6Qyw8W8jud"
```

参考资料：

[如何爬网易云音乐的评论数](https://www.zhihu.com/question/36081767)

[用java实现aes加密](http://blog.csdn.net/hbcui1984/article/details/5201247)
