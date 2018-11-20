package com.virex.admclient.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Работа с сетью. Форумы
 */
public interface ForumWebService {
    //http://www.delphimaster.ru/cgi-bin/client.pl?allservers=1
    //http://www.delphimaster.ru/cgi-bin/client.pl?getforums=1
    //http://www.delphimaster.ru/cgi-bin/client.pl?getnew=lastmod&n=0
    //http://www.delphimaster.ru/cgi-bin/client.pl?getconf=id&n=0&from=0&to=-1
    //http://www.delphimaster.ru/cgi-bin/client.pl?anketa=id

    @GET("/cgi-bin/client.pl")
    Call<ResponseBody>  getForums(@Query("getforums") int count);
}
