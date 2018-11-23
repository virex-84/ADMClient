package com.virex.admclient.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Работа с сетью. Анкета
 */
public interface AnketaWebService {
    //http://www.delphimaster.ru/cgi-bin/client.pl?allservers=1
    //http://www.delphimaster.ru/cgi-bin/client.pl?getforums=1
    //http://www.delphimaster.ru/cgi-bin/client.pl?getnew=lastmod&n=0
    //http://www.delphimaster.ru/cgi-bin/client.pl?getconf=id&n=0&from=0&to=-1
    //http://www.delphimaster.ru/cgi-bin/client.pl?anketa=id

    @GET("/cgi-bin/client.pl")
    Call<ResponseBody>  getAnketa(@Query("anketa") int id);

    @FormUrlEncoded
    @POST("/cgi-bin/anketa.pl")
    Call<ResponseBody> checkLogin(
            @Field(value="login", encoded=true) String login,
            @Field(value="psw", encoded=true) String password,
            @Field(value="edit", encoded=true) String edit
    );
}
