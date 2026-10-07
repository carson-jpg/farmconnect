package com.farmconnect.config;

import com.farmconnect.model.*;
import com.farmconnect.repository.PostRepository;
import com.farmconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * First start only: a few general-guidance posts so the "Learn" screen is not empty in a demo.
 * They are authored by the admin account and can be deleted from the app. No fake businesses or phone numbers are created.
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class ContentSeeder implements CommandLineRunner {
    private final PostRepository posts;
    private final UserRepository users;

    @Override
    public void run(String... args) {
        if (posts.count() > 0) return;
        User admin = users.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElse(null);
        if (admin == null) return;
        add(admin, PostType.ANNOUNCEMENT, "GENERAL", "Welcome to FarmConnect",
                "FarmConnect connects farmers in Trans Nzoia with buyers, county officers and agricultural services.\n\n"
                        + "• Verify your identity and farm so buyers can see your produce.\n"
                        + "• Keep your farm records in the app: planting, harvest, expenses and income.\n"
                        + "• Join or create a farmer group or cooperative.\n"
                        + "• Check the weather advice before spraying, planting or drying your harvest.\n\n"
                        + "County officers will post trainings, programmes and opportunities here.");
        add(admin, PostType.TIP, "SOIL_WATER", "Test your soil before buying fertiliser",
                "A soil test tells you which nutrients and how much lime your field really needs, so you do not pay for fertiliser "
                        + "the crop cannot use. Collect small samples from several spots in the field, mix them, and take them to a "
                        + "soil-testing laboratory or ask your county extension officer how to arrange a test.");
        add(admin, PostType.TIP, "GENERAL", "Write everything down: why farm records pay off",
                "Farmers who record what they plant, spend and harvest can see which crops really make money, plan the next "
                        + "season and show a clear history to banks, SACCOs and buyers. Use the Records section of this app after "
                        + "every purchase, planting and harvest. It takes one minute and protects you from guessing.");
        add(admin, PostType.ADVISORY, "POST_HARVEST", "Dry grain well before storing it",
                "Maize and other cereals stored while still damp can grow mould and develop aflatoxin, which is harmful to people "
                        + "and animals and can make a harvest unsellable. Dry grain until it is safe for storage (about 13.5% moisture "
                        + "for maize), keep it off the bare floor on pallets, and use clean, dry, well-ventilated stores or hermetic bags. "
                        + "Ask your extension officer how to check moisture.");
        add(admin, PostType.TIP, "PESTS_DISEASES", "Use pesticides safely",
                "Always read the label, wear gloves, a mask and long clothes, never spray against the wind, and keep to the waiting "
                        + "period before harvest. Never reuse empty pesticide containers for food or water. Do not spray when rain is "
                        + "expected: check the Weather section first.");
    }

    private void add(User author, PostType type, String topic, String title, String body) {
        Post p = new Post();
        p.setAuthor(author);
        p.setType(type);
        p.setTopic(topic);
        p.setTitle(title);
        p.setBody(body);
        p.setAudience("ALL");
        posts.save(p);
    }
}
