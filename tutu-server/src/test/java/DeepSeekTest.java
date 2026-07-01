import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


public class DeepSeekTest {
    @Test
    void chatDeepseek(){
        OpenAiChatModel.OpenAiChatModelBuilder  builder = new OpenAiChatModel.OpenAiChatModelBuilder();
        OpenAiChatModel model = builder.apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com").modelName("deepseek-v4-flash")
                .build();

        String chat = model.chat("今天天气怎么样？");
        System.out.println(chat);
    }
}
