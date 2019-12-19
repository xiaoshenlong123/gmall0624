package com.atguigu.gmall0624.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0624.bean.SkuLsInfo;

import com.atguigu.gmall0624.bean.SkuLsParams;
import com.atguigu.gmall0624.bean.SkuLsResult;
import com.atguigu.gmall0624.service.ListService;
import com.atguigu.gmall0624.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.swing.text.Highlighter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService{

    // 调用JestClient
    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";


    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {

        /*
            put /index/type/1
                {
                    "id":"1",
                    "skuName":"小米手机"
                }
            1.  先定义动作
            2.  执行
         */
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        // 1.dsl语句
        // 2.定义执行动作
        // 3.执行并获取返回结果集
        // dsl语句
        String query = makeQueryStringForSearch(skuLsParams);
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 4.对执行过dsl查询后的返回结果进行封装
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);
        // 5.返回数据
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        // 借助redis
        Jedis jedis = redisUtil.getJedis();

        // String 数据类型来存储访问的次数 INCRBY  |  Zset 排序
        String key = "hotScore";
        // 加完之后的数据
        Double hotScore = jedis.zincrby(key, 1, "skuId:" + skuId);

        if (hotScore%10==0){
            // 更新一次es
            /*
            POST /gmall/SkuInfo/40/_update
                {
                  "doc": {
                    "hotScore": 10
                  }
                }
             */
            updateHotScore(skuId,  Math.round(hotScore));
        }
    }

    //更新es
    private void updateHotScore(String skuId, long hotScore) {
         /*
        1.  定义dsl 语句
        2.  定义执行的动作并执行
         */
        String updDsl = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}\n";

        Update update = new Update.Builder(updDsl).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //制作返回值数据:
    private SkuLsResult makeResultForSearch(SearchResult searchResult,SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();
        // 页面显示商品信息
        // List<SkuLsInfo> skuLsInfoList;
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        // 给skuLsInfoList赋值 从es的查询结果集中获取！
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if(hits!=null&&hits.size()>0){
            // 循环遍历
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                // 如果全文检索：则skuame 并非高亮字段 ，获取高亮的skuName {highlight}
                if (hit.highlight!=null && hit.highlight.size()>0){
                    List<String> list = hit.highlight.get("skuName");
                    String skuNameHI = list.get(0);
                    skuLsInfo.setSkuName(skuNameHI);
                }
                // 将skuLsInfo数据放入集合
                skuLsInfoList.add(skuLsInfo);
            }
        }
        // 将结果集中的skuLsInfo添加到集合
        // 该集合用于页面渲染
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        // 查询出来的总条数
        //long total;
        skuLsResult.setTotal(searchResult.getTotal());
        // 总页数
        //long totalPages;
        // 10 3 4 | 9 3 3
        // long totalPages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
        // 平台属性值Id 集合
        // 通过两张表多表关联就可以查询平台属性，平台属性值！
        //List<String> attrValueIdList;
        List<String> attrValueIdList = new ArrayList<>();
        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        if(buckets!=null&&buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                String vauleId = bucket.getKey();
                attrValueIdList.add(vauleId);
            }
        }
        // 将封装好的平台属性值id放入集合中
        skuLsResult.setAttrValueIdList(attrValueIdList);
        // 返回最终结果
        return skuLsResult;
    }

    //java编写动态dsl语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // dsl语句中最大的一对{}叫做查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //{query -- bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //{filter-->term} 需要进行判断是三级分类Id还是平台属性值id
        if(skuLsParams.getCatalog3Id()!=null&&skuLsParams.getCatalog3Id().length()>0){
            //"filter": [{"term": {"catalog3Id": "61"}}
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            //{bool-->filter-->term}
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // 判断平台属性值Id是否为空
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            //循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                //{"term": {"skuAttrValueList.valueId": "61"}}
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                //{bool --> filter--term}
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //用户第一次查询是通过全文检索的方式进行查询的！
        //{bool-->must}通过全文检索查询数据！
        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            /*"must": [
            {"match": {
                "skuName": "手机"
            }}
            ]*/
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            //{bool -- must -- match}
            boolQueryBuilder.must(matchQueryBuilder);
            //判断高亮
            /*"highlight": {
                "pre_tags": ["<span style=color:red>"],
                "fields": {"skuName": {}},
                "post_tags": ["</span>"]
            }*/
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.field("skuName");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlight(highlightBuilder);
        }
        // 设置排序sort
        /*"sort": [
        {
            "hotScore": {
            "order": "desc"
        }
        }
        ]*/
        searchSourceBuilder.sort("hotScore",SortOrder.DESC);
        // 分页
        /*"from": 0
                , "size": 2*/
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        // 每页的大小 | 查询之前修改每页显示的条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        // 聚合 平台属性值过滤
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);
        //{query --> bool}
        searchSourceBuilder.query(boolQueryBuilder);

        //返回dsl语句字符串
        String query = searchSourceBuilder.toString();
        System.out.println("query:"+query);
        return query;
    }
}
