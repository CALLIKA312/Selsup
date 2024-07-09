import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;



/* CALLIKA
    Для работы кода использовались 2 сторонние библиотеки:
    1. OkHttpClient, для выполнения HTTP POST запроса
    2. Jackson для JSON-сериализации, с помощью ObjectMapper
    Так же добавлена с токеном для аутентификации
*/

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final int requestLimit;
    private final long intervalMillis;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    private final String authToken;

    public CrptApi(TimeUnit timeUnit, int requestLimit, String authToken) {
        this.requestLimit = requestLimit;
        this.intervalMillis = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient();
        this.authToken = authToken;

        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()), intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        try {
            String jsonDocument = objectMapper.writeValueAsString(document);
            RequestBody body = RequestBody.create(jsonDocument, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Signature", signature)
                    .build();

            Call call = httpClient.newCall(request);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
            }
        } finally {
            semaphore.release();
        }
    }

    public static void main(String[] args) {
        //TODO изменить на требуемый токен
        String authToken = "your_auth_token_here";
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10, authToken);

        Document doc = new Document();
        doc.description = new Document.Description();
        doc.description.participantInn = "1234567890";
        doc.doc_id = "123";
        doc.doc_status = "status";
        doc.doc_type = "LP_INTRODUCE_GOODS";
        doc.importRequest = true;
        doc.owner_inn = "1234567890";
        doc.participant_inn = "1234567890";
        doc.producer_inn = "1234567890";
        doc.production_date = "2020-01-23";
        doc.production_type = "type";
        doc.products = new Document.Product[1];
        doc.products[0] = new Document.Product();
        doc.products[0].certificate_document = "cert_doc";
        doc.products[0].certificate_document_date = "2020-01-23";
        doc.products[0].certificate_document_number = "cert_num";
        doc.products[0].owner_inn = "1234567890";
        doc.products[0].producer_inn = "1234567890";
        doc.products[0].production_date = "2020-01-23";
        doc.products[0].tnved_code = "tnved";
        doc.products[0].uit_code = "uit";
        doc.products[0].uitu_code = "uitu";
        doc.reg_date = "2020-01-23";
        doc.reg_number = "reg_num";

        try {
            api.createDocument(doc, "signature");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}
