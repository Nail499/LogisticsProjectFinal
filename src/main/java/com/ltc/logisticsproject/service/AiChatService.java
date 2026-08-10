package com.ltc.logisticsproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ltc.logisticsproject.dto.SupportChatMessage;
import com.ltc.logisticsproject.entity.Role;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

// AI dəstək chat-i — Groq-un OpenAI-uyğun "chat completions" API-sinə tool
// çağırışı (function calling) ilə müraciət edir ki, bot uydurma cavab
// əvəzinə real sistem datasına əsaslansın (bax AiToolExecutor — hər tool rol
// üzrə scoped-dir). Söhbət tarixçəsi backend-də saxlanılmır (stateless) —
// frontend hər sorğuda bütün tarixçəni göndərir (bax SupportChatController,
// SupportChatWidget.jsx).
//
// Nə üçün Anthropic yox, Groq: Groq-un pulsuz tier-i kredit kartı tələb
// etmir (yalnız dəqiqəlik/günlük sorğu limitləri var, aşağı trafikli dəstək
// chat-i üçün praktik olaraq kifayət qədər genişdir). Groq açıq modelləri
// (Llama və s.) çox sürətli GPU-alternativ "LPU" avadanlığında işlədir.
//
// Konfiqurasiya: application.properties-də groq.api-key=${GROQ_API_KEY}
// (Stripe/mail açarları ilə eyni naxış — env dəyişəni kimi qoyulur, kodda
// sərt yazılmır, https://console.groq.com-dan pulsuz alınır). Açar hələ
// qoyulmayıbsa chat() sadə xəbərdarlıq mesajı qaytarır (tətbiqi aşağı sala
// bilməz).
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatService {

    static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    // Tool-use dövrü sonsuz getməsin deyə sərt limit — AI 5 dəfə alət
    // çağırıb hələ də yekun cavab verməyibsə, imtina mesajı qaytarılır.
    static final int MAX_TOOL_ROUNDS = 5;

    @Value("${groq.api-key:}")
    String apiKey;
    // llama-3.3-70b-versatile — Groq-un tool-calling dəstəkləyən modelləri
    // arasında ən keyfiyyətlisi, Azərbaycan dilini də qəbul edilən səviyyədə
    // başa düşür/yazır. env ilə override edilə bilər (bax GROQ_MODEL).
    @Value("${groq.model:llama-3.3-70b-versatile}")
    String model;

    final AiToolExecutor aiToolExecutor;
    final ObjectMapper objectMapper = new ObjectMapper();
    final RestTemplate restTemplate = new RestTemplate();

    public String chat(Role role, Long roleEntityId, List<SupportChatMessage> history) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI dəstək chat-i hələ konfiqurasiya edilməyib (GROQ_API_KEY yoxdur). Zəhmət olmasa dispetçerlə əlaqə saxlayın.";
        }
        if (history == null || history.isEmpty()) {
            return "Sualınızı yaza bilərsiniz.";
        }

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt(role));
        messages.add(systemMsg);
        for (SupportChatMessage m : history) {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent());
            messages.add(msg);
        }

        ArrayNode tools = buildTools(role);

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JsonNode response;
            try {
                response = callGroq(tools, messages);
            } catch (Exception e) {
                return "Üzr istəyirəm, AI xidmətinə qoşulmaq mümkün olmadı. Zəhmət olmasa bir az sonra yenidən cəhd edin.";
            }

            JsonNode choice = response.path("choices").path(0);
            JsonNode message = choice.path("message");
            String finishReason = choice.path("finish_reason").asText();
            JsonNode toolCalls = message.path("tool_calls");

            if (!"tool_calls".equals(finishReason) || !toolCalls.isArray() || toolCalls.isEmpty()) {
                String text = message.path("content").asText("");
                return text.isBlank() ? "Üzr istəyirəm, cavab hazırlaya bilmədim." : text;
            }

            // AI-ın tool_calls daxil olan mesajını olduğu kimi geri əlavə
            // edirik — OpenAI-uyğun API növbəti sorğuda tam kontekst gözləyir.
            messages.add(message);

            for (JsonNode call : toolCalls) {
                String toolCallId = call.path("id").asText();
                String toolName = call.path("function").path("name").asText();
                String argumentsRaw = call.path("function").path("arguments").asText("{}");

                JsonNode input;
                try {
                    input = objectMapper.readTree(argumentsRaw);
                } catch (Exception e) {
                    input = objectMapper.createObjectNode();
                }

                String resultText = aiToolExecutor.execute(role, roleEntityId, toolName, input);

                ObjectNode toolResultMsg = objectMapper.createObjectNode();
                toolResultMsg.put("role", "tool");
                toolResultMsg.put("tool_call_id", toolCallId);
                toolResultMsg.put("content", resultText);
                messages.add(toolResultMsg);
            }
        }

        return "Üzr istəyirəm, sualınızı emal edərkən problem yarandı. Zəhmət olmasa bir az sonra yenidən cəhd edin.";
    }

    private JsonNode callGroq(ArrayNode tools, ArrayNode messages) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.set("messages", messages);
        if (!tools.isEmpty()) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        String responseBody = restTemplate.postForObject(API_URL, entity, String.class);
        return objectMapper.readTree(responseBody);
    }

    // OpenAI-uyğun function-calling formatı: {"type":"function","function":
    // {"name":...,"description":...,"parameters": <JSON Schema>}} — Anthropic-in
    // "name"/"description"/"input_schema" sadə formatından fərqli olaraq
    // bir səviyyə əlavə "function" sarmalayıcısı var.
    private ArrayNode buildTools(Role role) {
        ArrayNode tools = objectMapper.createArrayNode();
        switch (role) {
            case CUSTOMER -> {
                tools.add(tool("list_my_orders",
                        "Müştərinin bütün sifarişlərinin siyahısını (tracking nömrəsi, status, təsvir, qiymət) qaytarır.",
                        emptySchema()));
                tools.add(tool("get_order_status",
                        "Müəyyən bir sifarişin (tracking nömrəsinə görə) ətraflı statusunu, sürücü/nəqliyyat vasitəsi məlumatını qaytarır.",
                        schemaWithString("trackingNumber", "Sifarişin tracking nömrəsi")));
            }
            case DRIVER -> {
                tools.add(tool("list_my_trips",
                        "Sürücünün hazırda aktiv (planlaşdırılmış/götürülmüş/yoldadır) reyslərinin siyahısını qaytarır.",
                        emptySchema()));
                tools.add(tool("get_trip_detail",
                        "Müəyyən bir reysin (ID-yə görə) ətraflı məlumatını qaytarır.",
                        schemaWithInteger("tripId", "Reysin ID-si")));
            }
            case DISPATCHER -> {
                tools.add(tool("get_fleet_summary",
                        "Bütün flotun ümumi vəziyyətini (gözləyən yüklər, aktiv reyslər, həll edilməmiş xəbərdarlıqlar) qaytarır.",
                        emptySchema()));
                tools.add(tool("list_active_trips",
                        "Hazırda aktiv olan bütün reyslərin qısa siyahısını (sürücü, nəqliyyat vasitəsi, status) qaytarır.",
                        emptySchema()));
            }
            default -> { /* ADMIN üçün alət yoxdur — bax SupportChatController, admin bu chat-ə çıxarılmır */ }
        }
        return tools;
    }

    private ObjectNode tool(String name, String description, ObjectNode parameters) {
        ObjectNode fn = objectMapper.createObjectNode();
        fn.put("name", name);
        fn.put("description", description);
        fn.set("parameters", parameters);

        ObjectNode t = objectMapper.createObjectNode();
        t.put("type", "function");
        t.set("function", fn);
        return t;
    }

    private ObjectNode emptySchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", objectMapper.createObjectNode());
        return schema;
    }

    private ObjectNode schemaWithString(String paramName, String paramDesc) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode param = objectMapper.createObjectNode();
        param.put("type", "string");
        param.put("description", paramDesc);
        props.set(paramName, param);
        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add(paramName);
        schema.set("required", required);
        return schema;
    }

    private ObjectNode schemaWithInteger(String paramName, String paramDesc) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode param = objectMapper.createObjectNode();
        param.put("type", "integer");
        param.put("description", paramDesc);
        props.set(paramName, param);
        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add(paramName);
        schema.set("required", required);
        return schema;
    }

    private String buildSystemPrompt(Role role) {
        String base = "Sən Fleetra logistika platformasının süni intellekt dəstək köməkçisisən. "
                + "Azərbaycan dilində, qısa və konkret cavab ver. Yalnız sənə verilmiş alətlərdən (tools) əldə "
                + "etdiyin real məlumata əsaslan, uydurma məlumat vermə. Əgər lazımi məlumatı ala bilmirsənsə, "
                + "bunu açıq deyib istifadəçini dispetçerlə əlaqə saxlamağa yönləndir.";
        return switch (role) {
            case CUSTOMER -> base + " Sən müştəriyə öz sifarişləri (çatdırılma statusu, sürücü məlumatı) haqqında kömək edirsən.";
            case DRIVER -> base + " Sən sürücüyə öz reysləri və tətbiqdən istifadə haqqında kömək edirsən.";
            case DISPATCHER -> base + " Sən dispetçerə flotun ümumi vəziyyəti haqqında kömək edirsən.";
            default -> base;
        };
    }
}
