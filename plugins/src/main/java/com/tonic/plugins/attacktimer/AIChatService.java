/*      */ package com.tonic.plugins.attacktimer;
/*      */ 
/*      */ import com.tonic.Logger;
/*      */ import java.io.IOException;
/*      */ import java.net.URI;
/*      */ import java.net.http.HttpClient;
/*      */ import java.net.http.HttpRequest;
/*      */ import java.net.http.HttpResponse;
/*      */ import java.time.Duration;
/*      */ import java.util.List;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ public class AIChatService
/*      */ {
/*   18 */   private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15L);
/*      */   
/*      */   private final HttpClient httpClient;
/*      */   
/*      */   private final String apiKey;
/*      */   private final String modelId;
/*      */   private final String provider;
/*      */   private final String apiType;
/*      */   private final String systemPrompt;
/*      */   
/*      */   public String getModelId() {
/*   29 */     return this.modelId;
/*      */   }
/*      */ 
/*      */   
/*      */   public String getProvider() {
/*   34 */     return this.provider;
/*      */   }
/*      */ 
/*      */   
/*      */   public String getSystemPrompt() {
/*   39 */     return this.systemPrompt;
/*      */   }
/*      */ 
/*      */   
/*      */   public AIChatService(String apiKey, String modelId, String provider, String apiType, String systemPrompt) {
/*   44 */     this.apiKey = apiKey;
/*   45 */     String rawModelId = (modelId != null && !modelId.trim().isEmpty()) ? modelId.trim() : "openai/gpt-oss-120b";
/*   46 */     String defaultApiType = (apiType != null && !apiType.trim().isEmpty()) ? apiType.trim().toLowerCase() : "together";
/*   47 */     this.modelId = validateAndNormalizeModel(rawModelId, defaultApiType);
/*   48 */     this.provider = (provider != null && !provider.trim().isEmpty()) ? provider : null;
/*   49 */     this.apiType = defaultApiType;
/*   50 */     this.systemPrompt = (systemPrompt != null && !systemPrompt.trim().isEmpty()) ? systemPrompt : null;
/*   51 */     this
/*      */       
/*   53 */       .httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String validateAndNormalizeModel(String modelId, String apiType) {
/*   62 */     if (modelId == null || modelId.trim().isEmpty())
/*      */     {
/*   64 */       return "openai/gpt-oss-120b";
/*      */     }
/*      */     
/*   67 */     modelId = modelId.trim();
/*      */ 
/*      */     
/*   70 */     if ("groq".equalsIgnoreCase(apiType)) {
/*      */ 
/*      */       
/*   73 */       String lowerModelId = modelId.toLowerCase();
/*      */ 
/*      */       
/*   76 */       if ("llama-4-scout".equalsIgnoreCase(modelId)) {
/*      */         
/*   78 */         Logger.norm("[AI Chatbot] Fixed invalid model name 'llama-4-scout' -> 'meta-llama/llama-4-scout-17b-16e-instruct'");
/*   79 */         return "meta-llama/llama-4-scout-17b-16e-instruct";
/*      */       } 
/*      */ 
/*      */       
/*   83 */       if (lowerModelId.contains("llama-4-scout") && !modelId.contains("/")) {
/*      */         
/*   85 */         Logger.norm("[AI Chatbot] Fixed incomplete model name '" + modelId + "' -> 'meta-llama/llama-4-scout-17b-16e-instruct'");
/*   86 */         return "meta-llama/llama-4-scout-17b-16e-instruct";
/*      */       } 
/*      */ 
/*      */ 
/*      */       
/*   91 */       return modelId;
/*      */     } 
/*      */ 
/*      */     
/*   95 */     if ("together".equalsIgnoreCase(apiType)) {
/*      */ 
/*      */       
/*   98 */       if ("openai/gpt-oss-120b".equalsIgnoreCase(modelId) || "gpt-oss-120b".equalsIgnoreCase(modelId))
/*      */       {
/*  100 */         return "openai/gpt-oss-120b";
/*      */       }
/*      */ 
/*      */ 
/*      */       
/*  105 */       return modelId;
/*      */     } 
/*      */ 
/*      */     
/*  109 */     return modelId;
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public String getResponse(String inputMessage) {
/*  119 */     return getResponse(inputMessage, false);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public String getResponseWithPrompt(String inputMessage, String customSystemPrompt) {
/*  130 */     return getResponseWithPrompt(inputMessage, customSystemPrompt, false);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   public String getResponseWithHistory(String inputMessage, List<AIChatMessage> history, String customSystemPrompt) {
/*  142 */     return getResponseWithHistory(inputMessage, history, customSystemPrompt, false);
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String getResponseWithHistory(String inputMessage, List<AIChatMessage> history, String customSystemPrompt, boolean isRetry) {
/*  150 */     if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
/*      */       
/*  152 */       Logger.norm("[AI Chatbot] API key not set - cannot generate response");
/*  153 */       return null;
/*      */     } 
/*      */     
/*  156 */     if (inputMessage == null || inputMessage.trim().isEmpty())
/*      */     {
/*  158 */       return null;
/*      */     }
/*      */     
/*      */     try {
/*      */       String apiUrl, jsonBody;
/*      */       
/*      */       int groqMaxTokens;
/*      */       double groqTemperature, groqTopP;
/*      */       int maxTokens;
/*  167 */       StringBuilder messagesBuilder = new StringBuilder("[");
/*  168 */       boolean first = true;
/*      */ 
/*      */       
/*  171 */       String promptToUse = (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty()) ? customSystemPrompt : this.systemPrompt;
/*  172 */       if (promptToUse != null && !promptToUse.trim().isEmpty()) {
/*      */         
/*  174 */         messagesBuilder.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(promptToUse)).append("\"}");
/*  175 */         first = false;
/*      */       } 
/*      */ 
/*      */       
/*  179 */       String username = null;
/*  180 */       if (promptToUse != null && promptToUse.contains("This conversation is with user '")) {
/*      */         
/*  182 */         int startIdx = promptToUse.indexOf("This conversation is with user '") + "This conversation is with user '".length();
/*  183 */         int endIdx = promptToUse.indexOf("'", startIdx);
/*  184 */         if (endIdx > startIdx)
/*      */         {
/*  186 */           username = promptToUse.substring(startIdx, endIdx);
/*      */         }
/*      */       } 
/*      */ 
/*      */       
/*  191 */       int maxHistoryMessages = 98;
/*  192 */       int startIndex = 0;
/*  193 */       if (history != null && history.size() > maxHistoryMessages)
/*      */       {
/*  195 */         startIndex = history.size() - maxHistoryMessages;
/*      */       }
/*      */ 
/*      */       
/*  199 */       if (username != null && history != null && !history.isEmpty()) {
/*      */         
/*  201 */         if (!first)
/*      */         {
/*  203 */           messagesBuilder.append(",");
/*      */         }
/*  205 */         messagesBuilder.append("{\"role\":\"system\",\"content\":\"This conversation is with user '").append(escapeJson(username)).append("'. Remember who you're talking to.\"}");
/*  206 */         first = false;
/*      */       } 
/*      */       
/*  209 */       if (history != null)
/*      */       {
/*  211 */         for (int i = startIndex; i < history.size(); i++) {
/*      */           
/*  213 */           AIChatMessage msg = history.get(i);
/*  214 */           if (msg.getRole() != null && msg.getContent() != null) {
/*      */             
/*  216 */             if (!first)
/*      */             {
/*  218 */               messagesBuilder.append(",");
/*      */             }
/*  220 */             messagesBuilder.append("{\"role\":\"").append(escapeJson(msg.getRole())).append("\",");
/*  221 */             messagesBuilder.append("\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
/*  222 */             first = false;
/*      */           } 
/*      */         } 
/*      */       }
/*      */ 
/*      */       
/*  228 */       if (!first)
/*      */       {
/*  230 */         messagesBuilder.append(",");
/*      */       }
/*  232 */       messagesBuilder.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(inputMessage)).append("\"}");
/*  233 */       messagesBuilder.append("]");
/*      */       
/*  235 */       String messagesJson = messagesBuilder.toString();
/*      */ 
/*      */       
/*  238 */       switch (this.apiType.toLowerCase()) {
/*      */         
/*      */         case "groq":
/*  241 */           apiUrl = "https://api.groq.com/openai/v1/chat/completions";
/*      */           
/*  243 */           groqMaxTokens = this.modelId.contains("moonshot") ? 4096 : 250;
/*  244 */           groqTemperature = this.modelId.contains("moonshot") ? 0.6D : 0.9D;
/*  245 */           groqTopP = this.modelId.contains("moonshot") ? 1.0D : 0.95D;
/*  246 */           jsonBody = "{\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + ",\"max_tokens\":" + groqMaxTokens + ",\"temperature\":" + groqTemperature + ",\"top_p\":" + groqTopP + ",\"stream\":false}";
/*      */           break;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         case "together":
/*  255 */           apiUrl = "https://api.together.xyz/v1/chat/completions";
/*      */           
/*  257 */           maxTokens = "openai/gpt-oss-120b".equals(this.modelId) ? 300 : 250;
/*  258 */           jsonBody = "{\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + ",\"max_tokens\":" + maxTokens + ",\"temperature\":0.9,\"top_p\":0.95}";
/*      */           break;
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         default:
/*  265 */           if (this.provider != null && !this.provider.trim().isEmpty()) {
/*      */             
/*  267 */             apiUrl = "https://router.huggingface.co/hf-inference/v1/chat/completions";
/*      */             
/*  269 */             jsonBody = "{\"provider\":\"" + escapeJson(this.provider) + "\",\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + "}";
/*      */ 
/*      */             
/*      */             break;
/*      */           } 
/*      */           
/*  275 */           apiUrl = "https://router.huggingface.co/hf-inference/api/models/" + this.modelId;
/*  276 */           jsonBody = "{\"inputs\":\"" + escapeJson(inputMessage) + "\"}";
/*      */           break;
/*      */       } 
/*      */ 
/*      */       
/*  281 */       Logger.norm("[AI Chatbot] Requesting " + this.apiType.toUpperCase() + " API with history: " + apiUrl);
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  287 */       HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Content-Type", "application/json").header("User-Agent", "VitaLite-AI-Chatbot/1.0").timeout(REQUEST_TIMEOUT);
/*      */       
/*  289 */       if (this.apiKey != null && !this.apiKey.trim().isEmpty())
/*      */       {
/*      */         
/*  292 */         requestBuilder.header("Authorization", "Bearer " + this.apiKey);
/*      */       }
/*      */       
/*  295 */       HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
/*  296 */       HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
/*      */       
/*  298 */       if (response.statusCode() == 200) {
/*      */         
/*  300 */         String responseBody = response.body();
/*  301 */         if (responseBody == null || responseBody.trim().isEmpty()) {
/*      */           
/*  303 */           Logger.norm("[AI Chatbot] API returned empty response body (status 200)");
/*  304 */           return null;
/*      */         } 
/*      */ 
/*      */         
/*  308 */         String parsedResponse = null;
/*  309 */         if (this.apiType.equalsIgnoreCase("groq") || this.apiType.equalsIgnoreCase("together")) {
/*      */           
/*  311 */           parsedResponse = parseOpenAIResponse(responseBody);
/*      */         }
/*  313 */         else if (apiUrl.contains("/chat/completions")) {
/*      */           
/*  315 */           parsedResponse = parseOpenAIResponse(responseBody);
/*      */         }
/*      */         else {
/*      */           
/*  319 */           parsedResponse = parseOldInferenceResponse(responseBody, inputMessage);
/*      */         } 
/*      */         
/*  322 */         if (parsedResponse == null || parsedResponse.trim().isEmpty()) {
/*      */           
/*  324 */           Logger.norm("[AI Chatbot] Failed to parse response. Response preview: " + ((responseBody.length() > 200) ? (responseBody.substring(0, 200) + "...") : responseBody));
/*      */ 
/*      */           
/*  327 */           if (!isRetry && this.apiType.equalsIgnoreCase("together") && this.modelId.equals("openai/gpt-oss-120b"))
/*      */           {
/*  329 */             Logger.norm("[AI Chatbot] Empty response from gpt-oss-120b - trying fallback model meta-llama/Llama-3-8b-chat-hf");
/*  330 */             String fallbackModel = "meta-llama/Llama-3-8b-chat-hf";
/*  331 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  332 */             return fallbackService.getResponseWithHistory(inputMessage, history, customSystemPrompt, true);
/*      */           }
/*      */         
/*      */         } else {
/*      */           
/*  337 */           Logger.norm("[AI Chatbot] Successfully parsed response (length: " + parsedResponse.length() + "): " + ((parsedResponse.length() > 50) ? (parsedResponse.substring(0, 50) + "...") : parsedResponse));
/*      */         } 
/*      */         
/*  340 */         return parsedResponse;
/*      */       } 
/*  342 */       if (response.statusCode() == 503) {
/*      */         
/*  344 */         Logger.norm("[AI Chatbot] Model is loading (503) - response unavailable");
/*  345 */         return null;
/*      */       } 
/*  347 */       if (response.statusCode() == 401) {
/*      */         
/*  349 */         Logger.norm("[AI Chatbot] Authentication failed (401) - check your API key");
/*  350 */         return null;
/*      */       } 
/*  352 */       if (response.statusCode() == 429) {
/*      */ 
/*      */         
/*  355 */         String str1 = response.body();
/*  356 */         boolean isRateLimit = (str1 != null && (str1.contains("Rate limit") || str1.contains("rate limit") || str1.contains("429") || str1.contains("TPD")));
/*      */         
/*  358 */         if (isRateLimit && !isRetry) {
/*      */           String fallbackModel;
/*      */ 
/*      */           
/*  362 */           if (this.apiType.equalsIgnoreCase("groq")) {
/*      */ 
/*      */             
/*  365 */             fallbackModel = "llama-3.1-8b-instant";
/*      */           }
/*  367 */           else if (this.apiType.equalsIgnoreCase("together")) {
/*      */ 
/*      */             
/*  370 */             fallbackModel = "meta-llama/Llama-3-8b-chat-hf";
/*      */           }
/*      */           else {
/*      */             
/*  374 */             fallbackModel = null;
/*      */           } 
/*      */           
/*  377 */           if (fallbackModel != null && !this.modelId.equals(fallbackModel)) {
/*      */             
/*  379 */             Logger.norm("[AI Chatbot] Rate limit reached for " + this.modelId + " - automatically falling back to " + fallbackModel);
/*      */             
/*  381 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  382 */             return fallbackService.getResponseWithHistory(inputMessage, history, customSystemPrompt, true);
/*      */           } 
/*      */         } 
/*      */ 
/*      */         
/*  387 */         String errorMsg = "[AI Chatbot] Rate limit reached (429) - " + ((str1 != null && str1.length() > 250) ? (str1.substring(0, 250) + "...") : str1);
/*  388 */         if (this.apiType.equalsIgnoreCase("together")) {
/*      */           
/*  390 */           errorMsg = errorMsg + " Try again later or switch to a different model in settings.";
/*      */         }
/*      */         else {
/*      */           
/*  394 */           errorMsg = errorMsg + " Try again tomorrow or switch to 'llama-3.1-8b-instant' in settings.";
/*      */         } 
/*  396 */         Logger.norm(errorMsg);
/*  397 */         return null;
/*      */       } 
/*  399 */       if (response.statusCode() == 404) {
/*      */ 
/*      */         
/*  402 */         String str = response.body();
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  408 */         boolean isModelNotFound = (str != null && (str.contains("model_not_found") || str.contains("does not exist") || str.contains("does not exist or") || str.contains("invalid_request_error") || (str.toLowerCase().contains("model") && str.toLowerCase().contains("not found"))));
/*      */ 
/*      */         
/*  411 */         if (isModelNotFound && !isRetry) {
/*      */           String fallbackModel;
/*      */ 
/*      */           
/*  415 */           if (this.apiType.equalsIgnoreCase("together")) {
/*      */             
/*  417 */             fallbackModel = "openai/gpt-oss-120b";
/*      */           }
/*  419 */           else if (this.apiType.equalsIgnoreCase("groq")) {
/*      */             
/*  421 */             fallbackModel = "llama-3.3-70b-versatile";
/*      */           }
/*      */           else {
/*      */             
/*  425 */             fallbackModel = "openai/gpt-oss-120b";
/*      */           } 
/*      */           
/*  428 */           if (!this.modelId.equals(fallbackModel)) {
/*      */             
/*  430 */             Logger.norm("[AI Chatbot] Model '" + this.modelId + "' not found (404) - automatically falling back to " + fallbackModel);
/*      */             
/*  432 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  433 */             return fallbackService.getResponseWithHistory(inputMessage, history, customSystemPrompt, true);
/*      */           } 
/*      */         } 
/*      */         
/*  437 */         Logger.norm("[AI Chatbot] API request failed with status 404 for URL: " + apiUrl + " | Response: " + ((
/*  438 */             str != null && str.length() > 200) ? 
/*  439 */             str.substring(0, 200) : str));
/*  440 */         return null;
/*      */       } 
/*      */ 
/*      */       
/*  444 */       String errorBody = response.body();
/*  445 */       Logger.norm("[AI Chatbot] API request failed with status " + response.statusCode() + " for URL: " + apiUrl + " | Response: " + ((
/*      */           
/*  447 */           errorBody != null && errorBody.length() > 200) ? 
/*  448 */           errorBody.substring(0, 200) : errorBody));
/*  449 */       return null;
/*      */     
/*      */     }
/*  452 */     catch (IOException e) {
/*      */       
/*  454 */       String errorMsg = e.getMessage();
/*      */       
/*  456 */       if (!isRetry && errorMsg != null && errorMsg.contains("GOAWAY")) {
/*      */         
/*  458 */         Logger.norm("[AI Chatbot] HTTP/2 connection issue (GOAWAY), retrying once...");
/*      */         
/*      */         try {
/*  461 */           Thread.sleep(1000L);
/*  462 */           return getResponseWithHistory(inputMessage, history, customSystemPrompt, true);
/*      */         }
/*  464 */         catch (InterruptedException ie) {
/*      */           
/*  466 */           Thread.currentThread().interrupt();
/*  467 */           return null;
/*      */         } 
/*      */       } 
/*  470 */       Logger.norm("[AI Chatbot] Network error: " + errorMsg);
/*  471 */       return null;
/*      */     }
/*  473 */     catch (InterruptedException e) {
/*      */       
/*  475 */       Thread.currentThread().interrupt();
/*  476 */       Logger.norm("[AI Chatbot] Request interrupted: " + e.getMessage());
/*  477 */       return null;
/*      */     }
/*  479 */     catch (Exception e) {
/*      */       
/*  481 */       Logger.norm("[AI Chatbot] Unexpected error: " + e.getMessage());
/*  482 */       return null;
/*      */     } 
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String getResponseWithPrompt(String inputMessage, String customSystemPrompt, boolean isRetry) {
/*  491 */     if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
/*      */       
/*  493 */       Logger.norm("[AI Chatbot] API key not set - cannot generate response");
/*  494 */       return null;
/*      */     } 
/*      */     
/*  497 */     if (inputMessage == null || inputMessage.trim().isEmpty())
/*      */     {
/*  499 */       return null;
/*      */     }
/*      */     
/*      */     try {
/*      */       String apiUrl, jsonBody, messagesJson;
/*      */       
/*      */       int groqMaxTokens;
/*      */       
/*      */       double groqTemperature, groqTopP;
/*      */       int maxTokens;
/*  509 */       String promptToUse = (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty()) ? customSystemPrompt : this.systemPrompt;
/*  510 */       if (promptToUse != null && !promptToUse.trim().isEmpty()) {
/*      */ 
/*      */         
/*  513 */         messagesJson = "[{\"role\":\"system\",\"content\":\"" + escapeJson(promptToUse) + "\"},{\"role\":\"user\",\"content\":\"" + escapeJson(inputMessage) + "\"}]";
/*      */       }
/*      */       else {
/*      */         
/*  517 */         messagesJson = "[{\"role\":\"user\",\"content\":\"" + escapeJson(inputMessage) + "\"}]";
/*      */       } 
/*      */ 
/*      */       
/*  521 */       switch (this.apiType.toLowerCase()) {
/*      */         
/*      */         case "groq":
/*  524 */           apiUrl = "https://api.groq.com/openai/v1/chat/completions";
/*      */           
/*  526 */           groqMaxTokens = this.modelId.contains("moonshot") ? 4096 : 250;
/*  527 */           groqTemperature = this.modelId.contains("moonshot") ? 0.6D : 0.9D;
/*  528 */           groqTopP = this.modelId.contains("moonshot") ? 1.0D : 0.95D;
/*  529 */           jsonBody = "{\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + ",\"max_tokens\":" + groqMaxTokens + ",\"temperature\":" + groqTemperature + ",\"top_p\":" + groqTopP + ",\"stream\":false}";
/*      */           break;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         case "together":
/*  538 */           apiUrl = "https://api.together.xyz/v1/chat/completions";
/*      */           
/*  540 */           maxTokens = "openai/gpt-oss-120b".equals(this.modelId) ? 300 : 250;
/*  541 */           jsonBody = "{\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + ",\"max_tokens\":" + maxTokens + ",\"temperature\":0.9,\"top_p\":0.95}";
/*      */           break;
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         default:
/*  548 */           if (this.provider != null && !this.provider.trim().isEmpty()) {
/*      */             
/*  550 */             apiUrl = "https://router.huggingface.co/hf-inference/v1/chat/completions";
/*      */             
/*  552 */             jsonBody = "{\"provider\":\"" + escapeJson(this.provider) + "\",\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + "}";
/*      */             
/*      */             break;
/*      */           } 
/*      */           
/*  557 */           apiUrl = "https://router.huggingface.co/hf-inference/api/models/" + this.modelId;
/*  558 */           jsonBody = "{\"inputs\":\"" + escapeJson(inputMessage) + "\"}";
/*      */           break;
/*      */       } 
/*      */ 
/*      */       
/*  563 */       Logger.norm("[AI Chatbot] Requesting " + this.apiType.toUpperCase() + " API: " + apiUrl);
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  569 */       HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Content-Type", "application/json").header("User-Agent", "VitaLite-AI-Chatbot/1.0").timeout(REQUEST_TIMEOUT);
/*      */       
/*  571 */       if (this.apiKey != null && !this.apiKey.trim().isEmpty())
/*      */       {
/*      */         
/*  574 */         requestBuilder.header("Authorization", "Bearer " + this.apiKey);
/*      */       }
/*      */       
/*  577 */       HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
/*  578 */       HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
/*      */       
/*  580 */       if (response.statusCode() == 200) {
/*      */         
/*  582 */         String responseBody = response.body();
/*  583 */         if (responseBody == null || responseBody.trim().isEmpty()) {
/*      */           
/*  585 */           Logger.norm("[AI Chatbot] API returned empty response body (status 200)");
/*  586 */           return null;
/*      */         } 
/*      */ 
/*      */         
/*  590 */         String parsedResponse = null;
/*  591 */         if (this.apiType.equalsIgnoreCase("groq") || this.apiType.equalsIgnoreCase("together")) {
/*      */           
/*  593 */           parsedResponse = parseOpenAIResponse(responseBody);
/*      */         }
/*  595 */         else if (apiUrl.contains("/chat/completions")) {
/*      */           
/*  597 */           parsedResponse = parseOpenAIResponse(responseBody);
/*      */         }
/*      */         else {
/*      */           
/*  601 */           parsedResponse = parseOldInferenceResponse(responseBody, inputMessage);
/*      */         } 
/*      */         
/*  604 */         if (parsedResponse == null || parsedResponse.trim().isEmpty()) {
/*      */           
/*  606 */           Logger.norm("[AI Chatbot] Failed to parse response. Response preview: " + ((responseBody.length() > 200) ? (responseBody.substring(0, 200) + "...") : responseBody));
/*      */ 
/*      */           
/*  609 */           if (!isRetry && this.apiType.equalsIgnoreCase("together") && this.modelId.equals("openai/gpt-oss-120b"))
/*      */           {
/*  611 */             Logger.norm("[AI Chatbot] Empty response from gpt-oss-120b - trying fallback model meta-llama/Llama-3-8b-chat-hf");
/*  612 */             String fallbackModel = "meta-llama/Llama-3-8b-chat-hf";
/*  613 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  614 */             return fallbackService.getResponseWithPrompt(inputMessage, customSystemPrompt, true);
/*      */           }
/*      */         
/*      */         } else {
/*      */           
/*  619 */           Logger.norm("[AI Chatbot] Successfully parsed response (length: " + parsedResponse.length() + "): " + ((parsedResponse.length() > 50) ? (parsedResponse.substring(0, 50) + "...") : parsedResponse));
/*      */         } 
/*      */         
/*  622 */         return parsedResponse;
/*      */       } 
/*  624 */       if (response.statusCode() == 503) {
/*      */         
/*  626 */         Logger.norm("[AI Chatbot] Model is loading (503) - response unavailable");
/*  627 */         return null;
/*      */       } 
/*  629 */       if (response.statusCode() == 401) {
/*      */         
/*  631 */         Logger.norm("[AI Chatbot] Authentication failed (401) - check your API key");
/*  632 */         return null;
/*      */       } 
/*  634 */       if (response.statusCode() == 429) {
/*      */ 
/*      */         
/*  637 */         String str1 = response.body();
/*  638 */         boolean isRateLimit = (str1 != null && (str1.contains("Rate limit") || str1.contains("rate limit") || str1.contains("429") || str1.contains("TPD")));
/*      */         
/*  640 */         if (isRateLimit && !isRetry) {
/*      */           String fallbackModel;
/*      */ 
/*      */           
/*  644 */           if (this.apiType.equalsIgnoreCase("groq")) {
/*      */ 
/*      */             
/*  647 */             fallbackModel = "llama-3.1-8b-instant";
/*      */           }
/*  649 */           else if (this.apiType.equalsIgnoreCase("together")) {
/*      */ 
/*      */             
/*  652 */             fallbackModel = "meta-llama/Llama-3-8b-chat-hf";
/*      */           }
/*      */           else {
/*      */             
/*  656 */             fallbackModel = null;
/*      */           } 
/*      */           
/*  659 */           if (fallbackModel != null && !this.modelId.equals(fallbackModel)) {
/*      */             
/*  661 */             Logger.norm("[AI Chatbot] Rate limit reached for " + this.modelId + " - automatically falling back to " + fallbackModel);
/*      */             
/*  663 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  664 */             return fallbackService.getResponseWithPrompt(inputMessage, customSystemPrompt, true);
/*      */           } 
/*      */         } 
/*      */ 
/*      */         
/*  669 */         String errorMsg = "[AI Chatbot] Rate limit reached (429) - " + ((str1 != null && str1.length() > 250) ? (str1.substring(0, 250) + "...") : str1);
/*  670 */         if (this.apiType.equalsIgnoreCase("together")) {
/*      */           
/*  672 */           errorMsg = errorMsg + " Try again later or switch to a different model in settings.";
/*      */         }
/*      */         else {
/*      */           
/*  676 */           errorMsg = errorMsg + " Try again tomorrow or switch to 'llama-3.1-8b-instant' in settings.";
/*      */         } 
/*  678 */         Logger.norm(errorMsg);
/*  679 */         return null;
/*      */       } 
/*  681 */       if (response.statusCode() == 404) {
/*      */ 
/*      */         
/*  684 */         String str = response.body();
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  690 */         boolean isModelNotFound = (str != null && (str.contains("model_not_found") || str.contains("does not exist") || str.contains("does not exist or") || str.contains("invalid_request_error") || (str.toLowerCase().contains("model") && str.toLowerCase().contains("not found"))));
/*      */ 
/*      */         
/*  693 */         if (isModelNotFound && !isRetry) {
/*      */           String fallbackModel;
/*      */ 
/*      */           
/*  697 */           if (this.apiType.equalsIgnoreCase("together")) {
/*      */             
/*  699 */             fallbackModel = "openai/gpt-oss-120b";
/*      */           }
/*  701 */           else if (this.apiType.equalsIgnoreCase("groq")) {
/*      */             
/*  703 */             fallbackModel = "llama-3.3-70b-versatile";
/*      */           }
/*      */           else {
/*      */             
/*  707 */             fallbackModel = "openai/gpt-oss-120b";
/*      */           } 
/*      */           
/*  710 */           if (!this.modelId.equals(fallbackModel)) {
/*      */             
/*  712 */             Logger.norm("[AI Chatbot] Model '" + this.modelId + "' not found (404) - automatically falling back to " + fallbackModel);
/*      */             
/*  714 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  715 */             return fallbackService.getResponseWithPrompt(inputMessage, customSystemPrompt, true);
/*      */           } 
/*      */         } 
/*      */         
/*  719 */         Logger.norm("[AI Chatbot] API request failed with status 404 for URL: " + apiUrl + " | Response: " + ((
/*  720 */             str != null && str.length() > 200) ? 
/*  721 */             str.substring(0, 200) : str));
/*  722 */         return null;
/*      */       } 
/*      */ 
/*      */       
/*  726 */       String errorBody = response.body();
/*  727 */       Logger.norm("[AI Chatbot] API request failed with status " + response.statusCode() + " for URL: " + apiUrl + " | Response: " + ((
/*      */           
/*  729 */           errorBody != null && errorBody.length() > 200) ? 
/*  730 */           errorBody.substring(0, 200) : errorBody));
/*  731 */       return null;
/*      */     
/*      */     }
/*  734 */     catch (IOException e) {
/*      */       
/*  736 */       String errorMsg = e.getMessage();
/*      */       
/*  738 */       if (!isRetry && errorMsg != null && errorMsg.contains("GOAWAY")) {
/*      */         
/*  740 */         Logger.norm("[AI Chatbot] HTTP/2 connection issue (GOAWAY), retrying once...");
/*      */         
/*      */         try {
/*  743 */           Thread.sleep(1000L);
/*  744 */           return getResponseWithPrompt(inputMessage, customSystemPrompt, true);
/*      */         }
/*  746 */         catch (InterruptedException ie) {
/*      */           
/*  748 */           Thread.currentThread().interrupt();
/*  749 */           return null;
/*      */         } 
/*      */       } 
/*  752 */       Logger.norm("[AI Chatbot] Network error: " + errorMsg);
/*  753 */       return null;
/*      */     }
/*  755 */     catch (InterruptedException e) {
/*      */       
/*  757 */       Thread.currentThread().interrupt();
/*  758 */       Logger.norm("[AI Chatbot] Request interrupted: " + e.getMessage());
/*  759 */       return null;
/*      */     }
/*  761 */     catch (Exception e) {
/*      */       
/*  763 */       Logger.norm("[AI Chatbot] Unexpected error: " + e.getMessage());
/*  764 */       return null;
/*      */     } 
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String getResponse(String inputMessage, boolean isRetry) {
/*  773 */     if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
/*      */       
/*  775 */       Logger.norm("[AI Chatbot] API key not set - cannot generate response");
/*  776 */       return null;
/*      */     } 
/*      */     
/*  779 */     if (inputMessage == null || inputMessage.trim().isEmpty())
/*      */     {
/*  781 */       return null;
/*      */     }
/*      */     
/*      */     try {
/*      */       String apiUrl, jsonBody, messagesJson;
/*      */       
/*      */       int groqMaxTokens;
/*      */       
/*      */       double groqTemperature, groqTopP;
/*      */       int maxTokens;
/*  791 */       if (this.systemPrompt != null && !this.systemPrompt.trim().isEmpty()) {
/*      */ 
/*      */         
/*  794 */         messagesJson = "[{\"role\":\"system\",\"content\":\"" + escapeJson(this.systemPrompt) + "\"},{\"role\":\"user\",\"content\":\"" + escapeJson(inputMessage) + "\"}]";
/*      */       }
/*      */       else {
/*      */         
/*  798 */         messagesJson = "[{\"role\":\"user\",\"content\":\"" + escapeJson(inputMessage) + "\"}]";
/*      */       } 
/*      */ 
/*      */       
/*  802 */       switch (this.apiType.toLowerCase()) {
/*      */         
/*      */         case "groq":
/*  805 */           apiUrl = "https://api.groq.com/openai/v1/chat/completions";
/*      */           
/*  807 */           groqMaxTokens = this.modelId.contains("moonshot") ? 4096 : 250;
/*  808 */           groqTemperature = this.modelId.contains("moonshot") ? 0.6D : 0.9D;
/*  809 */           groqTopP = this.modelId.contains("moonshot") ? 1.0D : 0.95D;
/*  810 */           jsonBody = "{\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + ",\"max_tokens\":" + groqMaxTokens + ",\"temperature\":" + groqTemperature + ",\"top_p\":" + groqTopP + ",\"stream\":false}";
/*      */           break;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         case "together":
/*  819 */           apiUrl = "https://api.together.xyz/v1/chat/completions";
/*      */           
/*  821 */           maxTokens = "openai/gpt-oss-120b".equals(this.modelId) ? 300 : 250;
/*  822 */           jsonBody = "{\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + ",\"max_tokens\":" + maxTokens + ",\"temperature\":0.9,\"top_p\":0.95}";
/*      */           break;
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*      */         default:
/*  829 */           if (this.provider != null && !this.provider.trim().isEmpty()) {
/*      */             
/*  831 */             apiUrl = "https://router.huggingface.co/hf-inference/v1/chat/completions";
/*      */             
/*  833 */             jsonBody = "{\"provider\":\"" + escapeJson(this.provider) + "\",\"model\":\"" + escapeJson(this.modelId) + "\",\"messages\":" + messagesJson + "}";
/*      */             
/*      */             break;
/*      */           } 
/*      */           
/*  838 */           apiUrl = "https://router.huggingface.co/hf-inference/api/models/" + this.modelId;
/*  839 */           jsonBody = "{\"inputs\":\"" + escapeJson(inputMessage) + "\"}";
/*      */           break;
/*      */       } 
/*      */ 
/*      */       
/*  844 */       Logger.norm("[AI Chatbot] Requesting " + this.apiType.toUpperCase() + " API: " + apiUrl);
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       
/*  850 */       HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Content-Type", "application/json").header("User-Agent", "VitaLite-AI-Chatbot/1.0").timeout(REQUEST_TIMEOUT);
/*      */       
/*  852 */       if (this.apiKey != null && !this.apiKey.trim().isEmpty())
/*      */       {
/*      */         
/*  855 */         requestBuilder.header("Authorization", "Bearer " + this.apiKey);
/*      */       }
/*      */       
/*  858 */       HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
/*  859 */       HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
/*      */       
/*  861 */       if (response.statusCode() == 200) {
/*      */         
/*  863 */         String responseBody = response.body();
/*  864 */         if (responseBody == null || responseBody.trim().isEmpty()) {
/*      */           
/*  866 */           Logger.norm("[AI Chatbot] API returned empty response body (status 200)");
/*  867 */           return null;
/*      */         } 
/*      */ 
/*      */         
/*  871 */         String parsedResponse = null;
/*  872 */         if (this.apiType.equalsIgnoreCase("groq") || this.apiType.equalsIgnoreCase("together")) {
/*      */           
/*  874 */           parsedResponse = parseOpenAIResponse(responseBody);
/*      */         }
/*  876 */         else if (apiUrl.contains("/chat/completions")) {
/*      */           
/*  878 */           parsedResponse = parseOpenAIResponse(responseBody);
/*      */         }
/*      */         else {
/*      */           
/*  882 */           parsedResponse = parseOldInferenceResponse(responseBody, inputMessage);
/*      */         } 
/*      */         
/*  885 */         if (parsedResponse == null || parsedResponse.trim().isEmpty()) {
/*      */           
/*  887 */           Logger.norm("[AI Chatbot] Failed to parse response. Response preview: " + ((responseBody.length() > 200) ? (responseBody.substring(0, 200) + "...") : responseBody));
/*      */ 
/*      */           
/*  890 */           if (!isRetry && this.apiType.equalsIgnoreCase("together") && this.modelId.equals("openai/gpt-oss-120b"))
/*      */           {
/*  892 */             Logger.norm("[AI Chatbot] Empty response from gpt-oss-120b - trying fallback model meta-llama/Llama-3-8b-chat-hf");
/*  893 */             String fallbackModel = "meta-llama/Llama-3-8b-chat-hf";
/*  894 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  895 */             return fallbackService.getResponse(inputMessage, true);
/*      */           }
/*      */         
/*      */         } else {
/*      */           
/*  900 */           Logger.norm("[AI Chatbot] Successfully parsed response (length: " + parsedResponse.length() + "): " + ((parsedResponse.length() > 50) ? (parsedResponse.substring(0, 50) + "...") : parsedResponse));
/*      */         } 
/*      */         
/*  903 */         return parsedResponse;
/*      */       } 
/*  905 */       if (response.statusCode() == 503) {
/*      */         
/*  907 */         Logger.norm("[AI Chatbot] Model is loading (503) - response unavailable");
/*  908 */         return null;
/*      */       } 
/*  910 */       if (response.statusCode() == 401) {
/*      */         
/*  912 */         Logger.norm("[AI Chatbot] Authentication failed (401) - check your API key");
/*  913 */         return null;
/*      */       } 
/*  915 */       if (response.statusCode() == 429) {
/*      */ 
/*      */         
/*  918 */         String str1 = response.body();
/*  919 */         boolean isRateLimit = (str1 != null && (str1.contains("Rate limit") || str1.contains("rate limit") || str1.contains("429") || str1.contains("TPD")));
/*      */         
/*  921 */         if (isRateLimit && !isRetry) {
/*      */           String fallbackModel;
/*      */ 
/*      */           
/*  925 */           if (this.apiType.equalsIgnoreCase("groq")) {
/*      */ 
/*      */             
/*  928 */             fallbackModel = "llama-3.1-8b-instant";
/*      */           }
/*  930 */           else if (this.apiType.equalsIgnoreCase("together")) {
/*      */ 
/*      */             
/*  933 */             fallbackModel = "meta-llama/Llama-3-8b-chat-hf";
/*      */           }
/*      */           else {
/*      */             
/*  937 */             fallbackModel = null;
/*      */           } 
/*      */           
/*  940 */           if (fallbackModel != null && !this.modelId.equals(fallbackModel)) {
/*      */             
/*  942 */             Logger.norm("[AI Chatbot] Rate limit reached for " + this.modelId + " - automatically falling back to " + fallbackModel);
/*      */             
/*  944 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  945 */             return fallbackService.getResponse(inputMessage, true);
/*      */           } 
/*      */         } 
/*      */ 
/*      */         
/*  950 */         String errorMsg = "[AI Chatbot] Rate limit reached (429) - " + ((str1 != null && str1.length() > 250) ? (str1.substring(0, 250) + "...") : str1);
/*  951 */         if (this.apiType.equalsIgnoreCase("together")) {
/*      */           
/*  953 */           errorMsg = errorMsg + " Try again later or switch to a different model in settings.";
/*      */         }
/*      */         else {
/*      */           
/*  957 */           errorMsg = errorMsg + " Try again tomorrow or switch to 'llama-3.1-8b-instant' in settings.";
/*      */         } 
/*  959 */         Logger.norm(errorMsg);
/*  960 */         return null;
/*      */       } 
/*  962 */       if (response.statusCode() == 404) {
/*      */ 
/*      */         
/*  965 */         String str = response.body();
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/*  971 */         boolean isModelNotFound = (str != null && (str.contains("model_not_found") || str.contains("does not exist") || str.contains("does not exist or") || str.contains("invalid_request_error") || (str.toLowerCase().contains("model") && str.toLowerCase().contains("not found"))));
/*      */ 
/*      */         
/*  974 */         if (isModelNotFound && !isRetry) {
/*      */           String fallbackModel;
/*      */ 
/*      */           
/*  978 */           if (this.apiType.equalsIgnoreCase("together")) {
/*      */             
/*  980 */             fallbackModel = "openai/gpt-oss-120b";
/*      */           }
/*  982 */           else if (this.apiType.equalsIgnoreCase("groq")) {
/*      */             
/*  984 */             fallbackModel = "llama-3.3-70b-versatile";
/*      */           }
/*      */           else {
/*      */             
/*  988 */             fallbackModel = "openai/gpt-oss-120b";
/*      */           } 
/*      */           
/*  991 */           if (!this.modelId.equals(fallbackModel)) {
/*      */             
/*  993 */             Logger.norm("[AI Chatbot] Model '" + this.modelId + "' not found (404) - automatically falling back to " + fallbackModel);
/*      */             
/*  995 */             AIChatService fallbackService = new AIChatService(this.apiKey, fallbackModel, this.provider, this.apiType, this.systemPrompt);
/*  996 */             return fallbackService.getResponse(inputMessage, true);
/*      */           } 
/*      */         } 
/*      */         
/* 1000 */         Logger.norm("[AI Chatbot] API request failed with status 404 for URL: " + apiUrl + " | Response: " + ((
/* 1001 */             str != null && str.length() > 200) ? 
/* 1002 */             str.substring(0, 200) : str));
/* 1003 */         return null;
/*      */       } 
/*      */ 
/*      */       
/* 1007 */       String errorBody = response.body();
/* 1008 */       Logger.norm("[AI Chatbot] API request failed with status " + response.statusCode() + " for URL: " + apiUrl + " | Response: " + ((
/*      */           
/* 1010 */           errorBody != null && errorBody.length() > 200) ? 
/* 1011 */           errorBody.substring(0, 200) : errorBody));
/* 1012 */       return null;
/*      */     
/*      */     }
/* 1015 */     catch (IOException e) {
/*      */       
/* 1017 */       String errorMsg = e.getMessage();
/*      */       
/* 1019 */       if (!isRetry && errorMsg != null && errorMsg.contains("GOAWAY")) {
/*      */         
/* 1021 */         Logger.norm("[AI Chatbot] HTTP/2 connection issue (GOAWAY), retrying once...");
/*      */         
/*      */         try {
/* 1024 */           Thread.sleep(1000L);
/* 1025 */           return getResponse(inputMessage, true);
/*      */         }
/* 1027 */         catch (InterruptedException ie) {
/*      */           
/* 1029 */           Thread.currentThread().interrupt();
/* 1030 */           return null;
/*      */         } 
/*      */       } 
/* 1033 */       Logger.norm("[AI Chatbot] Network error: " + errorMsg);
/* 1034 */       return null;
/*      */     }
/* 1036 */     catch (InterruptedException e) {
/*      */       
/* 1038 */       Thread.currentThread().interrupt();
/* 1039 */       Logger.norm("[AI Chatbot] Request interrupted: " + e.getMessage());
/* 1040 */       return null;
/*      */     }
/* 1042 */     catch (Exception e) {
/*      */       
/* 1044 */       Logger.norm("[AI Chatbot] Unexpected error: " + e.getMessage());
/* 1045 */       return null;
/*      */     } 
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String extractFinishReason(String responseBody) {
/*      */     try {
/* 1060 */       int finishReasonIdx = responseBody.indexOf("\"finish_reason\"");
/* 1061 */       if (finishReasonIdx == -1)
/*      */       {
/* 1063 */         return null;
/*      */       }
/*      */       
/* 1066 */       int colonIdx = responseBody.indexOf(":", finishReasonIdx);
/* 1067 */       if (colonIdx == -1)
/*      */       {
/* 1069 */         return null;
/*      */       }
/*      */       
/* 1072 */       int quoteStart = responseBody.indexOf("\"", colonIdx);
/* 1073 */       if (quoteStart == -1)
/*      */       {
/* 1075 */         return null;
/*      */       }
/*      */       
/* 1078 */       int quoteEnd = quoteStart + 1;
/* 1079 */       while (quoteEnd < responseBody.length()) {
/*      */         
/* 1081 */         if (responseBody.charAt(quoteEnd) == '"' && responseBody.charAt(quoteEnd - 1) != '\\') {
/*      */           break;
/*      */         }
/*      */         
/* 1085 */         quoteEnd++;
/*      */       } 
/*      */       
/* 1088 */       if (quoteEnd >= responseBody.length())
/*      */       {
/* 1090 */         return null;
/*      */       }
/*      */       
/* 1093 */       return responseBody.substring(quoteStart + 1, quoteEnd);
/*      */     }
/* 1095 */     catch (Exception e) {
/*      */       
/* 1097 */       return null;
/*      */     } 
/*      */   }
/*      */ 
/*      */   
/*      */   private String parseOpenAIResponse(String responseBody) {
/* 1103 */     if (responseBody == null || responseBody.trim().isEmpty()) {
/*      */       
/* 1105 */       Logger.norm("[AI Chatbot] parseOpenAIResponse: Response body is null or empty");
/* 1106 */       return null;
/*      */     } 
/*      */ 
/*      */     
/*      */     try {
/* 1111 */       int choicesIdx = responseBody.indexOf("\"choices\"");
/* 1112 */       if (choicesIdx == -1) {
/*      */         
/* 1114 */         Logger.norm("[AI Chatbot] parseOpenAIResponse: No 'choices' field found in response. Response: " + ((responseBody.length() > 300) ? (responseBody.substring(0, 300) + "...") : responseBody));
/* 1115 */         return null;
/*      */       } 
/*      */       
/* 1118 */       int contentIdx = responseBody.indexOf("\"content\"", choicesIdx);
/* 1119 */       if (contentIdx == -1) {
/*      */         
/* 1121 */         Logger.norm("[AI Chatbot] parseOpenAIResponse: No 'content' field found after 'choices'. Response: " + ((responseBody.length() > 500) ? (responseBody.substring(0, 500) + "...") : responseBody));
/* 1122 */         return null;
/*      */       } 
/*      */       
/* 1125 */       int colonIdx = responseBody.indexOf(":", contentIdx);
/* 1126 */       if (colonIdx == -1) {
/*      */         
/* 1128 */         Logger.norm("[AI Chatbot] parseOpenAIResponse: No colon found after 'content'");
/* 1129 */         return null;
/*      */       } 
/*      */       
/* 1132 */       int quoteStart = responseBody.indexOf("\"", colonIdx);
/* 1133 */       if (quoteStart == -1) {
/*      */         
/* 1135 */         Logger.norm("[AI Chatbot] parseOpenAIResponse: No opening quote found after colon");
/* 1136 */         return null;
/*      */       } 
/*      */       
/* 1139 */       int quoteEnd = quoteStart + 1;
/* 1140 */       while (quoteEnd < responseBody.length()) {
/*      */         
/* 1142 */         if (responseBody.charAt(quoteEnd) == '"' && responseBody.charAt(quoteEnd - 1) != '\\') {
/*      */           break;
/*      */         }
/*      */         
/* 1146 */         quoteEnd++;
/*      */       } 
/*      */       
/* 1149 */       if (quoteEnd >= responseBody.length()) {
/*      */         
/* 1151 */         Logger.norm("[AI Chatbot] parseOpenAIResponse: No closing quote found");
/* 1152 */         return null;
/*      */       } 
/*      */       
/* 1155 */       String result = responseBody.substring(quoteStart + 1, quoteEnd);
/*      */ 
/*      */ 
/*      */ 
/*      */       
/* 1160 */       result = result.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", " ").replace("\\r", " ").replace("\\t", " ");
/*      */       
/* 1162 */       result = result.trim();
/*      */       
/* 1164 */       if (result.isEmpty()) {
/*      */ 
/*      */         
/* 1167 */         String finishReason = extractFinishReason(responseBody);
/* 1168 */         if (finishReason != null) {
/*      */           
/* 1170 */           Logger.norm("[AI Chatbot] parseOpenAIResponse: Extracted content is empty. Finish reason: " + finishReason);
/*      */           
/* 1172 */           if (finishReason.contains("content_filter") || finishReason.contains("safety") || finishReason.contains("filter"))
/*      */           {
/* 1174 */             Logger.norm("[AI Chatbot] Content was filtered - this may indicate safety filters or model restrictions");
/*      */           }
/*      */         }
/*      */         else {
/*      */           
/* 1179 */           Logger.norm("[AI Chatbot] parseOpenAIResponse: Extracted content is empty (no finish_reason found). Full response preview: " + ((responseBody.length() > 500) ? (responseBody.substring(0, 500) + "...") : responseBody));
/*      */         } 
/* 1181 */         return null;
/*      */       } 
/*      */ 
/*      */ 
/*      */       
/* 1186 */       return result.isEmpty() ? null : result;
/*      */     }
/* 1188 */     catch (Exception e) {
/*      */       
/* 1190 */       Logger.norm("[AI Chatbot] Error parsing OpenAI response: " + e.getMessage());
/* 1191 */       return null;
/*      */     } 
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String parseOldInferenceResponse(String responseBody, String inputMessage) {
/* 1200 */     if (responseBody == null || responseBody.trim().isEmpty())
/*      */     {
/* 1202 */       return null;
/*      */     }
/*      */ 
/*      */     
/*      */     try {
/* 1207 */       int startIdx = responseBody.indexOf("\"generated_text\"");
/* 1208 */       if (startIdx == -1)
/*      */       {
/* 1210 */         return null;
/*      */       }
/*      */       
/* 1213 */       int colonIdx = responseBody.indexOf(":", startIdx);
/* 1214 */       if (colonIdx == -1)
/*      */       {
/* 1216 */         return null;
/*      */       }
/*      */       
/* 1219 */       int quoteStart = responseBody.indexOf("\"", colonIdx);
/* 1220 */       if (quoteStart == -1)
/*      */       {
/* 1222 */         return null;
/*      */       }
/*      */       
/* 1225 */       int quoteEnd = quoteStart + 1;
/* 1226 */       while (quoteEnd < responseBody.length()) {
/*      */         
/* 1228 */         if (responseBody.charAt(quoteEnd) == '"' && responseBody.charAt(quoteEnd - 1) != '\\') {
/*      */           break;
/*      */         }
/*      */         
/* 1232 */         quoteEnd++;
/*      */       } 
/*      */       
/* 1235 */       if (quoteEnd >= responseBody.length())
/*      */       {
/* 1237 */         return null;
/*      */       }
/*      */       
/* 1240 */       String result = responseBody.substring(quoteStart + 1, quoteEnd);
/*      */ 
/*      */ 
/*      */ 
/*      */       
/* 1245 */       result = result.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
/*      */       
/* 1247 */       if (inputMessage != null && result.startsWith(inputMessage))
/*      */       {
/* 1249 */         result = result.substring(inputMessage.length()).trim();
/*      */       }
/*      */       
/* 1252 */       result = result.trim();
/*      */ 
/*      */ 
/*      */       
/* 1256 */       return result.isEmpty() ? null : result;
/*      */     }
/* 1258 */     catch (Exception e) {
/*      */       
/* 1260 */       Logger.norm("[AI Chatbot] Error parsing old inference response: " + e.getMessage());
/* 1261 */       return null;
/*      */     } 
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String limitSentenceLength(String text) {
/* 1271 */     if (text == null || text.trim().isEmpty())
/*      */     {
/* 1273 */       return text;
/*      */     }
/*      */ 
/*      */ 
/*      */     
/* 1278 */     String[] sentences = text.split("(?<=[.!?])\\s+");
/*      */ 
/*      */     
/* 1281 */     if (sentences.length == 0 || (sentences.length == 1 && !text.matches(".*[.!?]")))
/*      */     {
/* 1283 */       sentences = new String[] { text };
/*      */     }
/*      */     
/* 1286 */     StringBuilder result = new StringBuilder();
/*      */     
/* 1288 */     for (int i = 0; i < sentences.length; i++) {
/*      */       
/* 1290 */       String sentence = sentences[i].trim();
/* 1291 */       if (!sentence.isEmpty()) {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */         
/* 1297 */         String[] words = sentence.split("\\s+");
/*      */ 
/*      */         
/* 1300 */         if (words.length != 0) {
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */           
/* 1306 */           int wordCount = Math.min(words.length, 10);
/* 1307 */           if (wordCount < 1)
/*      */           {
/* 1309 */             wordCount = 1;
/*      */           }
/*      */ 
/*      */           
/* 1313 */           StringBuilder limitedSentence = new StringBuilder();
/* 1314 */           for (int j = 0; j < wordCount; j++) {
/*      */             
/* 1316 */             if (j > 0)
/*      */             {
/* 1318 */               limitedSentence.append(" ");
/*      */             }
/*      */             
/* 1321 */             String word = words[j].replaceAll("[.!?]$", "");
/* 1322 */             limitedSentence.append(word);
/*      */           } 
/*      */ 
/*      */           
/* 1326 */           if (wordCount >= words.length) {
/*      */ 
/*      */             
/* 1329 */             String lastWord = words[words.length - 1];
/* 1330 */             if (lastWord.matches(".*[.!?]$"))
/*      */             {
/* 1332 */               String punctuation = lastWord.substring(lastWord.length() - 1);
/* 1333 */               limitedSentence.append(punctuation);
/*      */             
/*      */             }
/*      */             else
/*      */             {
/* 1338 */               limitedSentence.append(".");
/*      */             }
/*      */           
/*      */           }
/*      */           else {
/*      */             
/* 1344 */             limitedSentence.append(".");
/*      */           } 
/*      */           
/* 1347 */           if (result.length() > 0)
/*      */           {
/* 1349 */             result.append(" ");
/*      */           }
/* 1351 */           result.append(limitedSentence.toString());
/*      */         } 
/*      */       } 
/* 1354 */     }  return result.toString().trim();
/*      */   }
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   
/*      */   private String escapeJson(String input) {
/* 1362 */     if (input == null)
/*      */     {
/* 1364 */       return "";
/*      */     }
/*      */     
/* 1367 */     return input.replace("\\", "\\\\")
/* 1368 */       .replace("\"", "\\\"")
/* 1369 */       .replace("\n", "\\n")
/* 1370 */       .replace("\r", "\\r")
/* 1371 */       .replace("\t", "\\t");
/*      */   }
/*      */ }


/* Location:              /home/guccifur/Bureaublad/AttackTimerPlugin.jar!/com/tonic/plugins/attacktimer/AIChatService.class
 * Java compiler version: 11 (55.0)
 * JD-Core Version:       1.1.3
 */