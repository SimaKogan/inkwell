package com.inkwell.seed;

import com.inkwell.catalog.Author;
import com.inkwell.catalog.AuthorRepository;
import com.inkwell.catalog.CatalogItem;
import com.inkwell.catalog.CatalogItemRepository;
import com.inkwell.common.ContentChangedEvent;
import com.inkwell.social.*;
import com.inkwell.user.Role;
import com.inkwell.user.User;
import com.inkwell.user.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Fills an empty database with a small multilingual demo (INKWELL_SEED=true). Demo password: "demo12345". */
@Component
@ConditionalOnProperty(prefix = "inkwell.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final CatalogItemRepository items;
    private final AuthorRepository authors;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository likes;
    private final FollowRepository follows;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final PlatformTransactionManager tx;

    public DemoDataSeeder(UserRepository users, CatalogItemRepository items, AuthorRepository authors,
                          PostRepository posts, CommentRepository comments, PostLikeRepository likes,
                          FollowRepository follows, PasswordEncoder encoder, ApplicationEventPublisher events,
                          PlatformTransactionManager tx) {
        this.users = users; this.items = items; this.authors = authors; this.posts = posts;
        this.comments = comments; this.likes = likes; this.follows = follows; this.encoder = encoder;
        this.events = events; this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        new TransactionTemplate(tx).executeWithoutResult(status -> {
            if (users.count() > 0) return;
            seed();
            log.info("Demo data created. Sign in as demo / demo12345 (admin: admin / admin12345)");
        });
    }

    private void seed() {
        User admin = user("admin", "admin@inkwell.dev", "Inkwell Admin", "admin12345", Role.ADMIN);
        User demo = user("demo", "demo@inkwell.dev", "Demo Reader", "demo12345", Role.USER);
        User maya = user("maya", "maya@inkwell.dev", "Майя", "demo12345", Role.USER);
        User noam = user("noam", "noam@inkwell.dev", "נועם", "demo12345", Role.USER);
        follow(demo, maya); follow(demo, noam); follow(maya, noam); follow(noam, maya);

        CatalogItem master = item(admin, "Мастер и Маргарита", "Роман о визите дьявола в Москву 1930-х, о любви, трусости и рукописях, которые не горят.", "Книги", "9785170807", "14.90", 8, 1967, "Михаил Булгаков");
        CatalogItem crime = item(admin, "Преступление и наказание", "Психологический роман о студенте Раскольникове, теории «права на кровь» и пути к раскаянию.", "Книги", "9785170844", "12.50", 5, 1866, "Фёдор Достоевский");
        item(admin, "הנסיך הקטן", "מהדורה עברית של הסיפור הקלאסי על נסיך קטן שמטייל בין כוכבים ולומד מהי ידידות.", "Книги", "9789651601", "9.90", 12, 1943, "אנטואן דה סנט־אכזיפרי");
        item(admin, "The Pragmatic Programmer", "Practical advice on craftsmanship, tooling and thinking like a developer who ships.", "Книги", "9780135957", "39.00", 4, 2019, "David Thomas", "Andrew Hunt");
        item(admin, "Clean Architecture", "A guide to drawing boundaries between business rules and details such as frameworks and databases.", "Книги", "9780134494", "34.00", 6, 2017, "Robert C. Martin");
        item(admin, "Designing Data-Intensive Applications", "How storage engines, replication and stream processing really work, explained from first principles.", "Книги", "9781449373", "44.00", 3, 2017, "Martin Kleppmann");
        CatalogItem pen = item(admin, "Перьевая ручка Lamy Safari", "Лёгкая перьевая ручка с треугольной зоной хвата. Хорошо пишет на тонкой бумаге, подходит для ежедневных заметок.", "Канцелярия", null, "32.00", 15, null);
        item(admin, "Блокнот A5 в линейку", "120 страниц плотной бумаги 100 г/м², чернила не просвечивают. Линейка 7 мм.", "Канцелярия", null, "11.00", 40, null);
        item(admin, "Чернила синие, 50 мл", "Водорастворимые чернила для перьевых ручек, насыщенный синий оттенок, быстро сохнут.", "Канцелярия", null, "8.50", 25, null);
        item(admin, "עט נובע כחול", "עט נובע קל לשימוש יומיומי, מתאים לכתיבה על נייר דק.", "Канцелярия", null, "27.00", 9, null);
        item(admin, "Black Ink Calligraphy Set", "Dip pen, three nibs and a bottle of black ink for practising italic and copperplate scripts.", "Канцелярия", null, "24.00", 7, null);
        item(admin, "Тетрадь для прописей", "Пособие для тренировки почерка: направляющие линии, наклон 65°, образцы букв.", "Канцелярия", null, "6.00", 30, null);

        Post p1 = post(maya, master, "Какое издание «Мастера и Маргариты» выбрать?", "Хочу перечитать роман. Подскажите, у какого издания хорошие комментарии и нормальная печать? Читаю в основном по вечерам, так что шрифт важен.", PostSource.TYPED);
        post(demo, pen, "Lamy Safari: полгода спустя", "Пишу этой ручкой каждый день. Перо EF даёт тонкую линию, на дешёвой бумаге растекается, на хорошей всё идеально. Рекомендую брать вместе с конвертером.", PostSource.TYPED);
        post(noam, crime, "מה דעתכם על דוסטויבסקי?", "סיימתי את «פשע ועונש» בתרגום עברי. הקצב איטי בהתחלה, אבל הפרקים האחרונים עוצרי נשימה. מישהו קרא גם את «האחים קרמזוב»?", PostSource.TYPED);
        post(demo, null, "Заметка от руки", "Записал идеи на бумаге, потом отсканировал и распознал в Inkwell. Почерк у меня так себе, но распознало почти всё.", PostSource.HANDWRITTEN);

        comment(demo, p1, null, "Бери издание с комментариями Лидии Яновской, они очень помогают разобраться в контексте.");
        Comment reply = comment(maya, p1, null, "Спасибо! А шрифт там крупный?");
        comment(demo, p1, reply, "Да, вполне комфортный, плюс бумага не блестит.");
        likes.save(like(demo, p1));
        likes.save(like(noam, p1));
    }

    private User user(String username, String email, String display, String password, Role role) {
        User u = new User();
        u.setUsername(username); u.setEmail(email); u.setDisplayName(display);
        u.setPasswordHash(encoder.encode(password)); u.setRole(role);
        return users.save(u);
    }

    private CatalogItem item(User owner, String title, String desc, String category, String isbn, String price,
                             int stock, Integer year, String... authorNames) {
        CatalogItem c = new CatalogItem();
        c.setOwner(owner); c.setTitle(title); c.setDescription(desc); c.setCategory(category); c.setIsbn(isbn);
        c.setPrice(new BigDecimal(price)); c.setStock(stock); c.setPublishedYear(year);
        var set = new LinkedHashSet<Author>();
        for (String n : authorNames) set.add(authors.findByNameIgnoreCase(n).orElseGet(() -> authors.save(new Author(n))));
        c.setAuthors(set);
        items.save(c);
        events.publishEvent(new ContentChangedEvent("catalog_item", c.getId()));
        return c;
    }

    private Post post(User author, CatalogItem item, String title, String body, PostSource source) {
        Post p = new Post();
        p.setAuthor(author); p.setCatalogItem(item); p.setTitle(title); p.setBody(body); p.setSource(source);
        posts.save(p);
        events.publishEvent(new ContentChangedEvent("post", p.getId()));
        return p;
    }

    private Comment comment(User author, Post post, Comment parent, String body) {
        Comment c = new Comment();
        c.setAuthor(author); c.setPost(post); c.setParent(parent); c.setBody(body);
        return comments.save(c);
    }

    private PostLike like(User user, Post post) {
        PostLike l = new PostLike();
        l.setUser(user); l.setPost(post);
        return l;
    }

    private void follow(User a, User b) {
        Follow f = new Follow();
        f.setFollower(a); f.setFollowee(b);
        follows.save(f);
    }
}
