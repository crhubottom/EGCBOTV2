package com.egc.bot;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.audio.AudioResponseFormat;
import io.github.sashirestela.openai.domain.audio.SpeechRequest;
import io.github.sashirestela.openai.domain.audio.TranscriptionRequest;
import io.github.stefanbratanov.jvm.openai.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static com.egc.bot.Bot.keys;

public class AIController {
    public String gptCall(String prompt, String model){
        OpenAI openAI = OpenAI.newBuilder(keys.get("OPENAI_KEY")).build();
        ChatClient chatClient = openAI.chatClient();
        CreateChatCompletionRequest createChatCompletionRequest = CreateChatCompletionRequest.newBuilder()
                .model(model)
                .message(ChatMessage.userMessage(prompt))
                .build();
        ChatCompletion chatCompletion = chatClient.createChatCompletion(createChatCompletionRequest);
        String out = chatCompletion.toString();
        System.out.println(out);
        out = out.substring(out.indexOf("content=") + 8, out.lastIndexOf(", refusal"));
        return out;
    }
    public String gptCallWithSystem(String prompt,String systemPrompt,String model){
        OpenAI openAI = OpenAI.newBuilder(keys.get("OPENAI_KEY")).build();
        ChatClient chatClient = openAI.chatClient();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.systemMessage(systemPrompt));
        messages.add(ChatMessage.userMessage(prompt));
        CreateChatCompletionRequest createChatCompletionRequest = CreateChatCompletionRequest.newBuilder()
                .model(model)
                .messages(messages)
                .build();
        ChatCompletion chatCompletion = chatClient.createChatCompletion(createChatCompletionRequest);
        String out = chatCompletion.toString();
        out = out.substring(out.indexOf("content=") + 8, out.lastIndexOf(", refusal"));
        return out;
    }
    public boolean dalleCall(String prompt, String fileName){
        OpenAI openAI = OpenAI.newBuilder(keys.get("OPENAI_KEY")).build();
        ImagesClient imagesClient = openAI.imagesClient();
        CreateImageRequest createImageRequest = CreateImageRequest.newBuilder()
                .model("dall-e-3")
                .prompt(prompt)
                .build();
        Images images = imagesClient.createImage(createImageRequest);


        System.out.println(images.data());
        String j= images.data().toString();
        System.out.println(j);
        String link=j.substring(j.indexOf("url=")+4,j.lastIndexOf(", rev"));
        System.out.println(link);
        try(InputStream in = new URL(link).openStream()){
            Files.copy(in, Paths.get(fileName+".png"), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;

        }
    }
    public boolean ttsCall(String prompt, String fileName){
        int ran = (int) (Math.random() * 6);
        SpeechRequest.Voice voice = switch (ran) {
            case 0 -> SpeechRequest.Voice.ONYX;
            case 1 -> SpeechRequest.Voice.ALLOY;
            case 2 -> SpeechRequest.Voice.ECHO;
            case 3 -> SpeechRequest.Voice.NOVA;
            case 4 -> SpeechRequest.Voice.FABLE;
            case 5 -> SpeechRequest.Voice.SHIMMER;
            default -> SpeechRequest.Voice.ONYX;
        };
        var openAI = SimpleOpenAI.builder()
                .apiKey(keys.get("OPENAI_KEY"))
                .build();
        var speechRequest = SpeechRequest.builder()
                .model("tts-1")
                .input(prompt)
                .voice(voice)
                .responseFormat(SpeechRequest.SpeechResponseFormat.MP3)
                .speed(1.0)
                .build();
        var futureSpeech = openAI.audios().speak(speechRequest);
        var speechResponse = futureSpeech.join();
        try {
            var audioFile = new FileOutputStream(fileName+".mp3");
            audioFile.write(speechResponse.readAllBytes());
            System.out.println(audioFile.getChannel().size() + " bytes");
            audioFile.close();
            return true;
        }catch (IOException e){
            return false;
        }
    }
    public String voiceToText(String fileName){
        var openAI = SimpleOpenAI.builder()
                .apiKey(keys.get("OPENAI_KEY"))
                .build();
        var audioRequest = TranscriptionRequest.builder()
                .file(Paths.get(fileName+".wav"))
                .model("whisper-1")
                .responseFormat(AudioResponseFormat.VERBOSE_JSON)
                .temperature(0.2)
                .timestampGranularity(TranscriptionRequest.TimestampGranularity.WORD)
                .timestampGranularity(TranscriptionRequest.TimestampGranularity.SEGMENT)
                .build();
        var futureAudio = openAI.audios().transcribe(audioRequest);
        var audioResponse = futureAudio.join();
        return audioResponse.getText();
    }

}
