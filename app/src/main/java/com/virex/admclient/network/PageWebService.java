package com.virex.admclient.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Работа с сетью. Посты
 */
public interface PageWebService {
    //http://www.delphimaster.ru/cgi-bin/client.pl?allservers=1
    //http://www.delphimaster.ru/cgi-bin/client.pl?getforums=1
    //http://www.delphimaster.ru/cgi-bin/client.pl?getnew=lastmod&n=0
    //http://www.delphimaster.ru/cgi-bin/client.pl?getconf=id&n=0&from=0&to=-1
    //http://www.delphimaster.ru/cgi-bin/client.pl?anketa=id

    @GET("/cgi-bin/client.pl")
    Call<ResponseBody> getPages(@Query("getconf") int id, @Query("n") int forum_id, @Query("from") int from, @Query("to") int to);

    @GET("/cgi-bin/client.pl")
    Call<ResponseBody> getPages2(@Query("getconf2") int id, @Query("n") int forum_id, @Query("from") int from, @Query("to") int to);

    @FormUrlEncoded
    @POST("/cgi-bin/forum.pl")
    Call<ResponseBody> addPost( @Field("n") String n,
                  @Field("id") String id,
                  @Field(value="name", encoded=true) String name,
                  @Field(value="topsw", encoded=true) String topsw,
                  @Field(value="email", encoded=true) String email,
                  @Field(value="signature", encoded=true) String signature,
                  @Field(value="text", encoded=true) String text, //признак encoded=true - кодировать не нужно
                  @Field(value="add2", encoded=true) String add2, //признак encoded=true - кодировать не нужно
                  @Field("p") String p
    );
}
