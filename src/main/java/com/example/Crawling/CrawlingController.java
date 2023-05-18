package com.example.Crawling;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.HashMap;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;

@RestController
public class CrawlingController {
    //조회할 URL 
    //네이버 뉴스 검색어:개발자, 옵션: 관련도순
    public String[] url = { 
        //"https://search.naver.com/search.naver?where=news&sm=tab_pge&query=%EA%B0%9C%EB%B0%9C%EC%9E%90&sort=0&photo=0&field=0&pd=0&ds=&de=&cluster_rank=41&mynews=0&office_type=0&office_section_code=0&news_office_checked=&nso=so:r,p:all,a:all&start=1",
        //"https://search.naver.com/search.naver?where=news&sm=tab_pge&query=%EA%B0%9C%EB%B0%9C%EC%9E%90&sort=0&photo=0&field=0&pd=0&ds=&de=&cluster_rank=66&mynews=0&office_type=0&office_section_code=0&news_office_checked=&nso=so:r,p:all,a:all&start=11",
        "https://search.naver.com/search.naver?where=news&sm=tab_pge&query=%EA%B0%9C%EB%B0%9C%EC%9E%90&sort=0&photo=0&field=0&pd=0&ds=&de=&cluster_rank=41&mynews=0&office_type=0&office_section_code=0&news_office_checked=&nso=so:r,p:all,a:all&start=21"
    };
    
    //페이지에서 URL 추출하기      
    @GetMapping("api/crawling/UrlList")         
    public List<String> UrlList() {     
        List<String> mergedList = new ArrayList<>();
        for (String el : url) {                       
            try {
                Connection conn = Jsoup.connect(el);
                Document document = null;            
                document = conn.get();

                List<String> list = getDataUrlList(document);
                mergedList.addAll(list);
                
            } catch (IOException e) {
                e.printStackTrace();
            }                
        }               
        return mergedList;
    }

    public List<String> getDataUrlList(Document document) {
        List<String> list = new ArrayList<>();

        //태그중 class가 .news_tit인 태그만 추출
        Elements selects = document.select(".news_tit");

        //해당 테그에서 href 속성값을 추출
        for (Element select : selects) {
            list.add((select.attr("href")).toString());
        }
        return list;
    }

    //페이지 텍스트 추출하기        
    @GetMapping("api/crawling/UrlText")   
    public List<String> UrlText() {
        List<String> urlList = UrlList();
        
        List<String> mergedList = new ArrayList<>();
    
        for (String el : urlList) {            
            try {
                Connection conn = Jsoup.connect(el);
                Document document = null;
                document = conn.get();

                List<String> list = getDataList(document);

                mergedList.addAll(list);
            } catch (IOException e) {
                e.printStackTrace();
            }                                                
        }

        return mergedList;
    }

    public List<String> getDataList(Document document) {
        List<String> list = new ArrayList<>();
        //div or p 태그만 선택
        List<Element> selects = document.select("div,p");

        //각 태그에 텍스트 추출
        for (Element select : selects) {
            list.add((select.text()).toString());
        }
        return list;
    }

    //텍스트 형태소 분석        
    public List<String> getVocaList(String text){        
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);        

        KomoranResult anayResultList = komoran.analyze(text);

        //일반명사(NNG), 대명사(NNP), 의존명사(NNB), 외국어(SL)만 추출
        List<String> list = anayResultList.getMorphesByTags("NNG", "NNP", "NNB", "SL");

        return list;
    }

    //웹 크롤링 및 형태소 분석
    @GetMapping("api/crawling/getCrawlingList") 
    public HashMap<String, Integer> getCrawlingList(){
        List<String> textList = UrlText();                
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        Integer count = 0;
        
        for(String text : textList){
            List<String> list = getVocaList(text);  
            
            for(String val : list){         
                count++;       
                if(map.get(val) != null){
                    map.put(val,map.get(val)+1);
                }
                else{
                    map.put(val,1);
                }  
                
                if(count>1000){
                    return map;
                }
            }
        }

        return map;
    }
}
