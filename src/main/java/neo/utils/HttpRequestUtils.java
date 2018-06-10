package neo.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpRequestUtils {

    private static Log logger = LogFactory.getLog(HttpRequestUtils.class);

    public static final int CONNECT_TIMEOUT = 30000;                                    // 连接超时
    public static final int SOCKET_TIMEOUT = 30000;                                    // 数据获取超时

    private static PoolingHttpClientConnectionManager connMgr;
    private static RequestConfig requestConfig;

    static {
        // 设置连接池
        connMgr = new PoolingHttpClientConnectionManager();
        // 设置连接池大小
        connMgr.setMaxTotal(100);
        connMgr.setDefaultMaxPerRoute(connMgr.getMaxTotal());
        requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
    }

    /**
     * http post请求示例
     *
     * @return response body的json串
     */
    public static String httpInvoke(String requestURL, String jsonBody) {
        String responseStr = "";
        try {
            HttpClient httpClient = null;
            if (requestURL != null && requestURL.startsWith("https://")) {
                System.setProperty("jsse.enableSNIExtension", "false");
                httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())
                        .setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
            } else {
                httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            }
            HttpPost httpRequest = new HttpPost(requestURL);
            httpRequest.addHeader("Accept", "application/json;charset=utf-8");
            httpRequest.addHeader("Content-Type", "application/json;charset=utf-8");
            httpRequest.addHeader("Content-Encoding", "utf-8");
            if (!StringUtils.isEmpty(jsonBody)) {
                httpRequest.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            }
            try {
//                logger.info("--------------request--------------");
//                logger.info(jsonBody);
                HttpResponse httpResponse = httpClient.execute(httpRequest);
                responseStr = EntityUtils.toString(httpResponse.getEntity(), Charset.forName("UTF-8"));
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            } finally {
//                logger.info("--------------response--------------");
                logger.info(responseStr);
//                logger.info("--------------request-response end--------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }
        return responseStr;
    }

    public static String httpBaseAuthInvoke(String requestURL, Map<String, String> params, String client, String password) {

        String responseStr = "";
        HttpClient httpClient = null;
        if (requestURL != null && requestURL.startsWith("https://")) {
            System.setProperty("jsse.enableSNIExtension", "false");
            httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())
                    .setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        } else {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        }
        String tok = client + ":" + password;
        String authorization = Base64.encodeBase64String(tok.getBytes());
        HttpPost httpRequest = new HttpPost(requestURL);
        httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpRequest.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        httpRequest.addHeader("Authorization", "Basic " + authorization);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        //装填参数
        if (params != null) {
            //装填参数
            for (Map.Entry<String, String> entry : params.entrySet()) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        try {
            //设置参数到请求对象中
            httpRequest.setEntity(new UrlEncodedFormEntity(nvps, "utf8"));
            logger.info("--------------request--------------");
            logger.info(params.toString());
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            responseStr = EntityUtils.toString(httpResponse.getEntity(), Charset.forName("UTF-8"));
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            logger.info("--------------response--------------");
            logger.info(responseStr);
            logger.info("--------------request-response end--------------");
        }
        return responseStr;
    }

    public static String httpBaseAuthInvoke(String requestURL, String token) {
        String responseStr = "";
        HttpClient httpClient = null;
        if (requestURL != null && requestURL.startsWith("https://")) {
            System.setProperty("jsse.enableSNIExtension", "false");
            httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())
                    .setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        } else {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        }
        HttpPost httpRequest = new HttpPost(requestURL);
        httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpRequest.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        httpRequest.addHeader("Authorization", "bearer " + token);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        try {
            //设置参数到请求对象中
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            responseStr = EntityUtils.toString(httpResponse.getEntity(), Charset.forName("UTF-8"));
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            logger.info("--------------response--------------");
            logger.info(responseStr);
            logger.info("--------------request-response end--------------");
        }
        return responseStr;
    }

//    public static void main(String[] args) {
//        Map<String, String> params = new HashMap<>();
//        params.put("redirect_uri","http://127.0.0.1:8031/open-api/connected-reddit");
//        params.put("code","3VodyrQy0z2KTkXX8EyHa6RiWjw");
//        params.put("grant_type","authorization_code");
//        String response = httpBaseAuthInvoke("https://www.reddit.com/api/v1/access_token",params,"mdcg1D2HYQX3Og","9OWdMcBjgjuIOyfAUxeiTGXxUh0");
//        System.out.println(response);
//
////        httpBaseAuthInvoke("https://www.reddit.com/api/v1/me", "32920724114-cXdvfz9RL3jsvsU1gEylBLRgEr8");
//    }


    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

                @Override
                public void verify(String host, SSLSocket ssl) throws IOException {
                }

                @Override
                public void verify(String host, X509Certificate cert) throws SSLException {
                }

                @Override
                public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
                }
            });
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return sslsf;
    }


    /**
     * *****************************************************************************************
     * ******************************** generate private key ************************************
     */

    public static String doPasswordDigest(String nonce, String created, String privateKey) {
        String passwordDigest = null;
        try {
            byte[] b1 = nonce != null ? nonce.getBytes("UTF-8") : new byte[0];
            byte[] b2 = created != null ? created.getBytes("UTF-8") : new byte[0];
            byte[] b3 = privateKey != null ? privateKey.getBytes("UTF-8") : new byte[0];
            byte[] b4 = new byte[b1.length + b2.length + b3.length];
            int offset = 0;
            System.arraycopy(b1, 0, b4, offset, b1.length);
            offset += b1.length;
            System.arraycopy(b2, 0, b4, offset, b2.length);
            offset += b2.length;
            System.arraycopy(b3, 0, b4, offset, b3.length);
            byte[] digestBytes = generateDigest(b4);
            passwordDigest = Base64.encodeBase64String(digestBytes);
        } catch (Exception e) {
            logger.error(e);
        }
        return passwordDigest;
    }

    public static byte[] generateDigest(byte[] inputBytes) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(inputBytes);
        } catch (Exception e) {
            throw new Exception("Error in generating digest", e);
        }
    }

    /**
     * *****************************************************************************************
     * ****************************** generate created stamp************************************
     */

    public static final String CREATED_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static String getCreated(Date date) {
        return getDateFormatter().format(date == null ? new Date() : date);
    }

    public static DateFormat getDateFormatter() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(CREATED_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    /**
     * ******************************************************************************************
     * ******************************** generate nonce str **************************************
     */

    public static String generateNonceStr(int length) throws Exception {
        return Base64.encodeBase64String(generateNonce(length));
    }

    private static byte[] generateNonce(int length) throws Exception {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            byte[] temp = new byte[length];
            random.nextBytes(temp);
            return temp;
        } catch (Exception ex) {
            throw new Exception("Error in generating nonce of length " + length, ex);
        }
    }

    public static String getConfigedURL() {
        //        return "http://test.api.deepbrain.ai:8383/deep-brain-api/ask";
        return "http://122.144.200.2:8383/deep-brain-api/ask";
    }
}
